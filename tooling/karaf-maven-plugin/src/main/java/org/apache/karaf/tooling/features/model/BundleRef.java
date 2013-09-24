/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.features.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;

public class BundleRef {
    String url;
    Integer startLevel;
    Artifact artifact;
    String bundleSymbolicName;
    String bundleVersion;

    public BundleRef(String url, Integer startLevel) {
        super();
        this.url = url;
        this.startLevel = startLevel;
    }

    public String getUrl() {
        return url;
    }

    public Integer getStartLevel() {
        return startLevel;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }
    
    public void readManifest() {
        JarInputStream bundleJar = null;
        try {
            File file = artifact.getFile();
            bundleJar = new JarInputStream(new FileInputStream(file));
            Manifest manifest = bundleJar.getManifest();
            bundleSymbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
            bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
            bundleJar.close();
        } catch (Exception e) {
            // Ignore errors in manifest
        }
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

}
