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
package org.apache.karaf.shell.ssh;

import java.io.File;

import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.common.file.nativefs.NativeSshFile;

/**
 * Karaf file system view reduced to the KARAF_BASE location
 */
public class KarafFileSystemView extends NativeFileSystemView {

    private String username;
    private String location;

    public KarafFileSystemView(String username) {
        super(username);
        this.username = username;
    }

    @Override
    public String getVirtualUserDir() {
        return "/";
    }

    @Override
    public String getPhysicalUserDir() {
        if (location == null) {
            location = new File(System.getProperty("karaf.base")).getAbsolutePath();
        }
        return location;
    }

    protected SshFile getFile(String dir, String file) {
        // get actual file object
        String location = getPhysicalUserDir();
        String physicalName = NativeSshFile.getPhysicalName(location, dir, file, false);
        if (!physicalName.startsWith(location)) {
            throw new IllegalArgumentException("The path is not relative to KARAF_BASE. For security reason, it's not allowed");
        }
        File fileObj = new File(physicalName);
        // strip the root directory and return
        String karafFileName = physicalName.substring(location.length());
        return createNativeSshFile(karafFileName, fileObj, username);
    }

}
