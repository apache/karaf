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
package org.apache.felix.obr.plugin;

/**
 * this class is used to store some user information about configuration of the plugin.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class Config {

    /**
     * use relative path or not.
     */
    private boolean m_pathRelative; // use relative or absolute path in repository.xml

    /**
     * deploy file or not.
     */
    private boolean m_fileRemote; // deploy file on remote server

    /**
     * constructor: set default configuration: use relative path and don't upload file.
     *
     */
    public Config() {
        // default configuration
        m_pathRelative = true;
        m_fileRemote = false;
    }

    /**
     * set relativePath attribute.
     * @param value new value of attribute
     */
    public void setPathRelative(boolean value) {
        m_pathRelative = value;
    }

    /**
     * set fileRemote attribute.
     * @param value new value of attribute
     */
    public void setRemotely(boolean value) {
        m_fileRemote = value;
    }

    /**
     * get use path relative.
     * @return true if plugin use relative path, else false
     */
    public boolean isPathRelative() {
        return m_pathRelative;
    }

    /**
     * get if use upload file.
     * @return true if the file will be uploaded, else false
     */
    public boolean isRemotely() {
        return m_fileRemote;
    }
}
