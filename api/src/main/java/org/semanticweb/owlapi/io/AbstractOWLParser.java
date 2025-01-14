/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.XZInputStream;
import org.xml.sax.InputSource;

/**
 * A convenience base class for parsers, which provides a mechanism to manage the setting and
 * getting of the {@code OWLOntologyManager} that should be associated with the parser. Note: all
 * current parser implementations are stateless.
 * 
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 2.0.0
 */
public abstract class AbstractOWLParser implements OWLParser, Serializable {

    private static final long serialVersionUID = 40000L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOWLParser.class);
    private static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String GZ_FILE_EXTENSION = ".gz";
    private static final String XZ_FILE_EXTENSION = ".xz";
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
    private static final Pattern CONTENT_DISPOSITION_FILE_NAME_PATTERN =
        Pattern.compile(".*filename=\"([^\\s;]*)\".*");
    private static final int CONTENT_DISPOSITION_FILE_NAME_PATTERN_GROUP = 1;
    private static final Pattern ZIP_ENTRY_ONTOLOGY_NAME_PATTERN =
        Pattern.compile(".*owl|rdf|xml|mos");
    private final String acceptableContentEncoding = "xz,gzip,deflate";
    private static final String TEXTPLAIN_REQUEST_TYPE = ", text/plain; q=0.1";
    private static final String LAST_REQUEST_TYPE = ", */*; q=0.09";
    protected static final String DEFAULT_REQUEST =
        "application/rdf+xml, application/xml; q=0.7, text/xml; q=0.6" + TEXTPLAIN_REQUEST_TYPE
            + LAST_REQUEST_TYPE;
    private static final String ACCEPTABLE_CONTENT_ENCODING = "xz,gzip,deflate";

    protected AbstractOWLParser() {}

    /**
     * A convenience method that obtains an input stream from a URI. This method sets up the correct
     * request type and wraps the input stream within a buffered input stream.
     * 
     * @param documentIRI The URI from which the input stream should be returned
     * @param config the load configuration
     * @param acceptHeaders accept headers
     * @return The input stream obtained from the URI
     * @throws IOException if there was an {@link java.io.IOException} in obtaining the input stream
     *         from the URI.
     */
    @Nonnull
    protected InputStream getInputStream(@Nonnull IRI documentIRI,
        @Nonnull OWLOntologyLoaderConfiguration config, String acceptHeaders) throws IOException {
        String actualAcceptHeaders = acceptHeaders;
        if (!acceptHeaders.contains("text/plain")) {
            actualAcceptHeaders += TEXTPLAIN_REQUEST_TYPE;
        }
        if (!acceptHeaders.contains("*/*")) {
            actualAcceptHeaders += LAST_REQUEST_TYPE;
        }

        URL originalURL = documentIRI.toURI().toURL();
        URLConnection conn = originalURL.openConnection();
        conn.addRequestProperty("Accept", actualAcceptHeaders);
        if (config.getAuthorizationValue() != null && !config.getAuthorizationValue().isEmpty()) {
            conn.setRequestProperty("Authorization", config.getAuthorizationValue());
        }
        if (config.isAcceptingHTTPCompression()) {
            conn.setRequestProperty("Accept-Encoding", acceptableContentEncoding);
        }
        int connectionTimeout = config.getConnectionTimeout();
        conn.setConnectTimeout(connectionTimeout);
        conn = connect(config, actualAcceptHeaders, conn, connectionTimeout, new HashSet<>());
        String contentEncoding = conn.getContentEncoding();
        String fileName = getFileNameFromContentDisposition(conn);
        if (fileName == null && conn.getURL() != null) {
            fileName = conn.getURL().toString();
        }
        InputStream is = null;
        int count = 0;
        while (count < config.getRetriesToAttempt() && is == null) {
            try {
                is = getInputStreamFromContentEncoding(fileName, conn, contentEncoding);
                if (is != null) {
                    return is;
                }
            } catch (SocketTimeoutException e) {
                count++;
                if (count == 5) {
                    throw e;
                }
                conn.setConnectTimeout(connectionTimeout + connectionTimeout * count);
            }
        }
        if (is == null) {
            throw new IOException("cannot connect to " + documentIRI + "; retry limit exhausted");
        }
        return checkFileName(fileName, is);
    }

    protected URLConnection connect(OWLOntologyLoaderConfiguration config,
        String actualAcceptHeaders, URLConnection conn, int connectionTimeout, Set<String> visited)
        throws IOException, MalformedURLException {
        if (conn instanceof HttpURLConnection && config.isFollowRedirects()) {
            // follow redirects to HTTPS
            HttpURLConnection con = (HttpURLConnection) conn;
            con.connect();
            int responseCode = con.getResponseCode();
            // redirect
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                // no constants for temporary and permanent redirect in HttpURLConnection
                || responseCode == 307 || responseCode == 308) {
                String location = con.getHeaderField("Location");
                if (visited.add(location)) {
                    URL newURL = new URL(location);
                    return connect(config, actualAcceptHeaders,
                        rebuildConnection(config, connectionTimeout, newURL, actualAcceptHeaders),
                        connectionTimeout, visited);
                } else {
                    throw new IllegalStateException(
                        "Infinite loop: redirect cycle detected. " + visited);
                }
            }
        }
        return conn;
    }

    protected static URLConnection rebuildConnection(OWLOntologyLoaderConfiguration config,
        int connectionTimeout, URL newURL, String acceptHeaders) throws IOException {
        URLConnection conn;
        conn = newURL.openConnection();
        conn.addRequestProperty("Accept", acceptHeaders);
        if (config.isAcceptingHTTPCompression()) {
            conn.setRequestProperty("Accept-Encoding", ACCEPTABLE_CONTENT_ENCODING);
        }
        conn.setConnectTimeout(connectionTimeout);
        return conn;
    }

    private static InputStream checkFileName(String fileName, InputStream in) throws IOException {
        if (isZipFileName(fileName)) {
            ZipInputStream zis = new ZipInputStream(in);
            ZipEntry entry = null;
            ZipEntry nextEntry = zis.getNextEntry();
            while (entry != null && nextEntry != null) {
                if (couldBeOntology(nextEntry)) {
                    entry = nextEntry;
                }
                nextEntry = zis.getNextEntry();
            }
            return zis;
        }
        if (isGzFileName(fileName)) {
            return new GZIPInputStream(in);
        }
        if (isXzFileName(fileName)) {
            return new XZInputStream(in);
        }
        return in;
    }

    private static boolean couldBeOntology(@Nullable ZipEntry zipEntry) {
        if (zipEntry == null) {
            return false;
        }
        return ZIP_ENTRY_ONTOLOGY_NAME_PATTERN.matcher(zipEntry.getName()).matches();
    }

    @Nonnull
    private static InputStream getInputStreamFromContentEncoding(String fileName,
        @Nonnull URLConnection conn, @Nullable String contentEncoding) throws IOException {
        InputStream is = null;
        InputStream connInputStream = conn.getInputStream();
        if (contentEncoding != null) {
            if ("xz".equals(contentEncoding)) {
                LOGGER.info("URL connection input stream is compressed using xz");
                is = new BufferedInputStream(
                    checkFileName(fileName, new XZInputStream(connInputStream)));
            } else if ("gzip".equals(contentEncoding)) {
                LOGGER.info("URL connection input stream is compressed using gzip");
                is = new BufferedInputStream(
                    checkFileName(fileName, new GZIPInputStream(connInputStream)));
            } else if ("deflate".equals(contentEncoding)) {
                LOGGER.info("URL connection input stream is compressed using deflate");
                is = OWLOntologyDocumentSourceBase.wrap(checkFileName(fileName,
                    new InflaterInputStream(connInputStream, new Inflater(true))));
            }
        }
        if (is == null) {
            return OWLOntologyDocumentSourceBase.wrap(checkFileName(fileName, connInputStream));
        }
        return is;
    }

    @Nullable
    private static String getFileNameFromContentDisposition(@Nonnull URLConnection connection) {
        String contentDispositionHeaderValue =
            connection.getHeaderField(CONTENT_DISPOSITION_HEADER);
        if (contentDispositionHeaderValue != null) {
            Matcher matcher =
                CONTENT_DISPOSITION_FILE_NAME_PATTERN.matcher(contentDispositionHeaderValue);
            if (matcher.matches()) {
                return matcher.group(CONTENT_DISPOSITION_FILE_NAME_PATTERN_GROUP);
            }
        }
        return null;
    }

    private static boolean isZipFileName(@Nonnull String fileName) {
        return fileName.toLowerCase(Locale.getDefault()).endsWith(ZIP_FILE_EXTENSION);
    }

    private static boolean isGzFileName(@Nonnull String fileName) {
        return fileName.toLowerCase(Locale.getDefault()).endsWith(GZ_FILE_EXTENSION);
    }

    private static boolean isXzFileName(@Nonnull String fileName) {
        return fileName.toLowerCase(Locale.getDefault()).endsWith(XZ_FILE_EXTENSION);
    }

    @Nonnull
    protected InputSource getInputSource(@Nonnull OWLOntologyDocumentSource documentSource,
        @Nonnull OWLOntologyLoaderConfiguration config) throws IOException {
        InputSource is;
        if (documentSource.isReaderAvailable()) {
            is = new InputSource(documentSource.getReader());
        } else if (documentSource.isInputStreamAvailable()) {
            is = new InputSource(documentSource.getInputStream());
        } else {
            if (documentSource.getDocumentIRI().getNamespace().startsWith("jar:")) {
                if (documentSource.getDocumentIRI().getNamespace().startsWith("jar:!")) {
                    String name = documentSource.getDocumentIRI().toString().substring(5);
                    if (!name.startsWith("/")) {
                        name = "/" + name;
                    }
                    is = new InputSource(getClass().getResourceAsStream(name));
                    is.setSystemId(documentSource.getDocumentIRI().toString());
                    return is;
                } else {
                    try {
                        is = new InputSource(
                            ((JarURLConnection) new URL(documentSource.getDocumentIRI().toString())
                                .openConnection()).getInputStream());
                        is.setSystemId(documentSource.getDocumentIRI().toString());
                        return is;
                    } catch (IOException e) {
                        throw new OWLParserException(e);
                    }
                }
            }
            Optional<String> headers = documentSource.getAcceptHeaders();
            if (headers.isPresent()) {
                is = new InputSource(
                    getInputStream(documentSource.getDocumentIRI(), config, headers.get()));
            } else {
                is = new InputSource(
                    getInputStream(documentSource.getDocumentIRI(), config, DEFAULT_REQUEST));
            }
        }
        is.setSystemId(documentSource.getDocumentIRI().toString());
        return is;
    }

    @Nonnull
    @Override
    public OWLDocumentFormat parse(IRI documentIRI, OWLOntology ontology) throws IOException {
        return parse(new IRIDocumentSource(documentIRI, null, null), ontology,
            ontology.getOWLOntologyManager().getOntologyLoaderConfiguration());
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
