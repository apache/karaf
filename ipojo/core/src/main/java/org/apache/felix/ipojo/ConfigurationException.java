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
package org.apache.felix.ipojo;

/**
 * This class defines the exception thrown when an instance cannot be configured correctly.
 * This exception occurs when component metadata are not correct.
 * @see Exception
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationException extends Exception {
    
    /**
     * Serialization Id.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * The component type on which the error occurs.
     * Uses the factory name.
     */
    private String m_type;
    
    /**
     * Creates a new configuration exception.
     * @param mes the error message
     * @param typ the component type
     * @see Exception#Exception(String)
     */
    ConfigurationException(String mes, String typ) {
        super(mes);
        m_type = typ;
    }
    
    /**
     * Creates a new configuration exception.
     * @param mes the error message
     * @see Exception#Exception(String)
     */
    public ConfigurationException(String mes) {
        super(mes);
    }
    
    /**
     * Gets the error message.
     * @return the error message.
     * @see java.lang.Throwable#getMessage()
     */
    public String getMessage() {
        if (m_type == null) {
            return super.getMessage();
        } else {
            return "The configuration is not correct for the type " + m_type + " : " + super.getMessage();
        }
    }

}
