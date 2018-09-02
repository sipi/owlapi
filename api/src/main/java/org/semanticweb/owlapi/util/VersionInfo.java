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
package org.semanticweb.owlapi.util;

/**
 * A place holder for the current release number.
 *
 * @author Matthew Horridge, The University Of Manchester, Information Management Group
 * @since 2.2.0
 */
public class VersionInfo {

    private static final VersionInfo INSTANCE = new VersionInfo();
    private final String version;

    protected VersionInfo() {
        String v = VersionInfo.class.getPackage().getImplementationVersion();
        if (v != null) {
            version = v;
        } else {
            version = "5.1.7";
        }
    }

    /**
     * @return the version info
     */
    public static VersionInfo getVersionInfo() {
        return INSTANCE;
    }

    /**
     * Gets a string that contains the version of this build. This is generated from the manifest of
     * the jar that this class is packaged in.
     *
     * @return The version info string (if available).
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets a message saying "Generated by the OWL API (version x.x.x)".
     *
     * @return The message.
     */
    public String getGeneratedByMessage() {
        return "Generated by the OWL API (version " + version
            + ") https://github.com/owlcs/owlapi/";
    }

    @Override
    public String toString() {
        return "The OWL API (version " + version + ')';
    }
}
