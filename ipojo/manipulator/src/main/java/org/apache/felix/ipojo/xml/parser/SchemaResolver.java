/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.xml.parser;

import java.io.IOException;
import java.net.URL;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Entity Resolver loading embedded XML Schemas.
 * This resolver avoid using a network connection to get schemas as they
 * are loaded from the manipulator jar file.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SchemaResolver implements EntityResolver {
    
    /**
     * Directory where embedded schemas are copied.
     */
    public static final String XSD_PATH = "xsd";

    /**
     * Resolves systemIds to use embedded schemas. The schemas are loaded from
     * the {@link SchemaResolver#XSD_PATH} directory with the current classloader.
     * @param publicId the publicId
     * @param systemId the systemId (Schema URL)
     * @return the InputSource to load the schemas or <code>null</code> if the schema
     * cannot be loaded (not embedded)
     * @throws SAXException cannot happen 
     * @throws IOException when the embedded resource cannot be read correctly
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException {
        
        URL url = getURL(systemId);
        if (url == null) {
            // Cannot found the resource
            return null;
        } else {
            return new InputSource(url.openStream());
        }
        
    }
    
    /**
     * Computes the local URL of the given system Id.
     * This URL is computed by trying to load the resource from
     * the current classloader. First, the last fragment (file name) of the system id
     * url is extracted and the file is loaded from the {@link SchemaResolver#XSD_PATH}
     * directory ('xsd/extracted') 
     * @param id the systemId to load
     * @return the URL to the resources or <code>null</code> if the resource cannot be found.
     */
    private URL getURL(String id) {
        int index = id.lastIndexOf('/');
        String fragment = id.substring(index);
        return this.getClass().getClassLoader().getResource(XSD_PATH + fragment);
    }

}
