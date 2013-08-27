/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.command;

import java.net.URI;

/**
 * Simple abstraction of a maven artifact to avoid external deps
 */
public class Artifact {
    String groupId;
    String artifactId;
    String version;
    String extension;
    String classifier;
    
    public Artifact(String coords) {
        String[] coordsAr = coords.split(":");
        if (coordsAr.length != 5) {
            throw new IllegalArgumentException("Maven URL " + coords + " is malformed or not complete");
        }
        this.groupId = coordsAr[0];
        this.artifactId = coordsAr[1];
        this.version = coordsAr[4];
        this.extension = coordsAr[2];
        this.classifier = coordsAr[3];
    }
    
    public Artifact(String coords, String version) {
        this(coords);
        this.version = version;
    }
    
    public URI getPaxUrlForArtifact(String version) {
        String uriSt = "mvn:" + this.groupId + "/" + this.artifactId + "/" + version + "/" + this.extension + "/" + this.classifier;
        try {
            return new URI(uriSt);
        } catch (Exception e) {
            return null;
        }
    }
}
