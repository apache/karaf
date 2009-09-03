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
package org.apache.felix.fileinstall.internal;

import java.io.File;

import org.apache.felix.fileinstall.ArtifactListener;

/**
 * An artifact that has been dropped into one watched directory.
 */
public class Artifact {

    private File path;
    private File jaredDirectory;
    private long lastModified = -1;
    private ArtifactListener listener;
    private File transformed;
    private long bundleId = -1;

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public File getJaredDirectory() {
        return jaredDirectory;
    }

    public void setJaredDirectory(File jaredDirectory) {
        this.jaredDirectory = jaredDirectory;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public ArtifactListener getListener() {
        return listener;
    }

    public void setListener(ArtifactListener listener) {
        this.listener = listener;
    }

    public File getTransformed() {
        return transformed;
    }

    public void setTransformed(File transformed) {
        this.transformed = transformed;
    }

    public long getBundleId() {
        return bundleId;
    }

    public void setBundleId(long bundleId) {
        this.bundleId = bundleId;
    }
}
