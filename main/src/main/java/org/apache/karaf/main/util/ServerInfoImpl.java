/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.karaf.main.util;

import java.io.File;
import java.net.URI;

import org.apache.karaf.info.ServerInfo;

/**
 * @version $Rev:$ $Date:$
 */
public class ServerInfoImpl implements ServerInfo {

    private final File base;
    private final File home;
    private final File data;
    private final File instances;
    private final String[] args;

    public ServerInfoImpl(String[] args, File base, File data, File home, File instances) {
        this.args = args;
        this.base = base;
        this.data = data;
        this.home = home;
        this.instances = instances;
    }

    @Override
    public File getHomeDirectory() {
        return home;
    }

    @Override
    public String resolveHomePath(String filename) {
        return resolveWithBase(home, filename).getAbsolutePath();
    }

    @Override
    public File resolveHome(String filename) {
        return resolveWithBase(home, filename);
    }

    @Override
    public URI resolveHome(URI uri) {
        return home.toURI().resolve(uri);
    }

    @Override
    public File getBaseDirectory() {
        return base;
    }

    @Override
    public String resolveBasePath(String filename) {
        return resolveWithBase(base, filename).getAbsolutePath();
    }

    @Override
    public File resolveBase(String filename) {
        return resolveWithBase(base, filename);
    }

    @Override
    public URI resolveBase(URI uri) {
        return base.toURI().resolve(uri);
    }

    @Override
    public File getDataDirectory() {
        return data;
    }

    @Override
    public File getInstancesDirectory() {
        return instances;
    }

    @Override
    public String[] getArgs() {
        return args.clone();
    }

    private File resolveWithBase(File baseDir, String filename) {
        File file = new File(filename);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(baseDir, filename);
    }

}
