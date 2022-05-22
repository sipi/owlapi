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
package org.semanticweb.owlapi.model;

import java.io.Serializable;

/**
 * Represents the concrete representation format of an ontology. The equality of an ontology format
 * is defined by the equals and hashCode method (not its identity).
 *
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 2.0.0
 */
public interface OWLDocumentFormat extends Serializable {

    /**
     * @return A unique key for this format.
     */
    String getKey();

    /**
     * Determines whether this format contains textual output, as opposed to binary output.
     *
     * @return True if this format represents a textual format, as opposed to a binary format.
     *         Defaults to true if not overridden.
     */
    boolean isTextual();

    /**
     * @return true if this format supports prefixes in its output
     */
    default boolean hasPrefixes() {
        return true;
    }

    /**
     * Some formats support relative IRIs in output (all parsers must create axioms with absolute
     * IRIs, but some formats allow relative IRIs, with the understanding that in parsing the base
     * IRI will be used to convert them to absolute IRIs). If a format does not support relative
     * IRIs, the renderer needs to know to output absolute IRIs. This defaults to true as this keeps
     * the existing behaviour.
     * 
     * @return true if relative IRIs are supported.
     */
    default boolean supportsRelativeIRIs() {
        return true;
    }
}
