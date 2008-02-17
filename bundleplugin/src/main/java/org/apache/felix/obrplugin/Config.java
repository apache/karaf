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
package org.apache.felix.obrplugin;


import java.net.URI;


/**
 * this class is used to store some user information about configuration of the plugin.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class Config
{
    private boolean m_pathRelative; // use relative or absolute path in repository.xml
    private boolean m_remoteFile; // deploy file on remote server
    private URI m_remoteBundle; // public address of deployed bundle


    /**
     * constructor: set default configuration: use relative path and don't upload file.
     */
    public Config()
    {
        // default configuration
        m_pathRelative = true;
        m_remoteFile = false;
        m_remoteBundle = null;
    }


    /**
     * @param value enable to use relative path
     */
    public void setPathRelative( boolean value )
    {
        m_pathRelative = value;
    }


    /**
     * @param value enable when uploading
     */
    public void setRemoteFile( boolean value )
    {
        m_remoteFile = value;
    }


    /**
     * @param value public address of deployed bundle
     */
    public void setRemoteBundle( URI value )
    {
        m_remoteBundle = value;
    }


    /**
     * @return true if plugin uses relative path, else false
     */
    public boolean isPathRelative()
    {
        return m_pathRelative;
    }


    /**
     * @return true if the file will be uploaded, else false
     */
    public boolean isRemoteFile()
    {
        return m_remoteFile;
    }


    /**
     * @return public address of deployed bundle
     */
    public URI getRemoteBundle()
    {
        return m_remoteBundle;
    }
}
