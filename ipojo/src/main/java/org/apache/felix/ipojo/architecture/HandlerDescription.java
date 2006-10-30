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
package org.apache.felix.ipojo.architecture;


/**
 * Handler Description.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class HandlerDescription {


    /**
     * Handler Class Name (i.e namespace).
     */
    private String m_handlerName;

    /**
     * Is the handler valid.
     */
    private boolean m_isValid;


    /**
     * Constructor.
     * @param name : handler name
     * @param isValid : is the handler valid
     */
    public HandlerDescription(String name, boolean isValid) {
        m_handlerName = name;
        m_isValid = isValid;
    }

    /**
     * @return true if the handler is valid.
     */
    public boolean isValid() { return m_isValid; }

    /**
     * @return the handler name (i.e. namespace).
     */
    public String getHandlerName() { return m_handlerName; }

    /**
     * @return the handler information.
     */
    public String getHandlerInfo() { return ""; };

}
