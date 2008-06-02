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
package org.apache.felix.deploymentadmin;

import java.util.BitSet;
import java.util.jar.Attributes;

import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * Objects of this class represent the meta data for a resource from a deployment package, this
 * can be either bundle resources or processed resources.
 */
public class AbstractInfo {

    private static final BitSet VALID_RESOURCE_PATH_CHARS;
    static
    {
        VALID_RESOURCE_PATH_CHARS = new BitSet();
        for ( int i = 'a'; i <= 'z'; i++ )
        {
            VALID_RESOURCE_PATH_CHARS.set( i );
        }
        for ( int i = 'A'; i <= 'Z'; i++ )
        {
            VALID_RESOURCE_PATH_CHARS.set( i );
        }
        for ( int i = '0'; i <= '9'; i++ )
        {
            VALID_RESOURCE_PATH_CHARS.set( i );
        }
        VALID_RESOURCE_PATH_CHARS.set( '.' );
        VALID_RESOURCE_PATH_CHARS.set( '-' );
        VALID_RESOURCE_PATH_CHARS.set( '_' );
        VALID_RESOURCE_PATH_CHARS.set( '/' );
    }

    private final String m_path;
    private final Attributes m_attributes;
    private final boolean m_missing;

    /**
     * Create an instance
     *
     * @param path Resource-id aka path of the resource
     * @param attributes Attributes containing the meta data of the resource
     * @throws DeploymentException If the specified attributes do not match the correct syntax for a deployment package resource.
     */
    public AbstractInfo(String path, Attributes attributes) throws DeploymentException {
        verifyEntryName(path);
        m_path = path;
        m_attributes = attributes;
        m_missing = parseBooleanHeader(attributes, Constants.DEPLOYMENTPACKAGE_MISSING);
    }

    /**
     * @return The path of the resource
     */
    public String getPath() {
        return m_path;
    }

    /**
     * Return the value of a header for this resource
     * @param header Name of the header
     * @return Value of the header specified by the given header name
     */
    public String getHeader(String header) {
        return m_attributes.getValue(header);
    }

    private void verifyEntryName(String name) throws DeploymentException {
        byte[] bytes = name.getBytes();
        boolean delimiterSeen = false;
        for(int j = 0; j < bytes.length; j++) {
            if(!VALID_RESOURCE_PATH_CHARS.get(bytes[j])) {
                throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Resource ID '" + name +"' contains invalid character(s)");
            }
            if (bytes[j] == '/') {
                if (delimiterSeen) {
                    throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Resource ID '" + name +"' contains multiple consequetive path seperators");
                } else {
                    delimiterSeen = true;
                }
            } else {
                delimiterSeen = false;
            }
        }
    }

    /**
     * Determine if a resource is missing or not
     * @return True if the actual data for this resource is not present, false otherwise
     */
    public boolean isMissing() {
        return m_missing;
    }

    /**
     * Parses a header that is allowed to have only boolean values.
     *
     * @param attributes Set of attributes containing the header
     * @param header The header to verify
     * @return true if the value of the header was "true", false if the value was "false"
     * @throws DeploymentException if the value was not "true" or "false"
     */
    protected boolean parseBooleanHeader(Attributes attributes, String header) throws DeploymentException {
        String value = attributes.getValue(header);
        if (value != null) {
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)){
                return false;
            } else {
                throw new DeploymentException(DeploymentException.CODE_BAD_HEADER, "Invalid '" + header + "' header for manifest " +
                    "entry '" + getPath() + "' header, should be either 'true' or 'false' or not present");
            }
        }
        return false;
    }

}
