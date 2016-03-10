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
package org.apache.karaf.main.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import org.apache.karaf.util.maven.Parser;

/**
 * Resolves local maven artifacts and raw file paths
 */
public class SimpleMavenResolver implements ArtifactResolver {
    private final List<File> mavenRepos;

    /**
     * 
     * @param mavenRepos list of base dirs of maven repos that should be used when resolving maven artifacts
     */
    public SimpleMavenResolver(List<File> mavenRepos) {
        this.mavenRepos = mavenRepos;
    }

    /**
     * Resolve from pax-url format for maven URIs to the file that is referenced by the URI
     * The URI format is:
     * mvn:&lt;groupId&gt;/&lt;artifactId&gt;/&lt;version&gt;/&lt;type&gt;/&lt;classifier&gt;
     * 
     * If artifactUri does not match the Syntax the local file that corresponds to the path is returned
     * 
     * @param artifactUri Maven artifact URI
     * @return resolved URI
     */
    public URI resolve(URI artifactUri) {
        for (File bundleDir : mavenRepos) {
            File file = findFile(bundleDir, artifactUri);
            if (file != null) {
                return file.toURI();
            }
        }
        throw new RuntimeException("Could not resolve " + artifactUri);
    }

    private static File findFile(File dir, URI mvnUri) {
        try {
            String path = Parser.pathFromMaven(mvnUri.toString());
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
            }

            File theFile = new File(dir, path);

            if (theFile.exists() && !theFile.isDirectory()) {
                return theFile;
            }
            return null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

}
