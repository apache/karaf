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
package org.apache.felix.dm.test.bundle.annotation.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.resources.Resource;

public class StaticResource implements Resource
{
    private String m_id;
    private String m_name;
    private String m_path;
    private String m_repository;

    public StaticResource(String name, String path, String repository) {
        m_id = repository + ":" + path + "/" + name;
        m_name = name;
        m_path = path;
        m_repository = repository;
    }
    
    public String getID() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public String getPath() {
        return m_path;
    }

    public String getRepository() {
        return m_repository;
    }
    
    public Dictionary getProperties() {
        return new Properties() {{
            put(Resource.ID, getID());
            put(Resource.NAME, getName());
            put(Resource.PATH, getPath());
            put(Resource.REPOSITORY, getRepository());
        }};
    }

    public InputStream openStream() throws IOException {
        return null;
    }
}
