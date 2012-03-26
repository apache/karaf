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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves local maven artifacts and raw file paths
 */
public class SimpleMavenResolver {
    private static final Pattern mvnPattern = Pattern.compile("mvn:([^/ ]+)/([^/ ]+)/([^/ ]*)(/([^/ ]+)(/([^/ ]+))?)?");
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
     * mvn:<groupId>/<artifactId>/<version>/<type>/<classifier>
     * 
     * If artifactUri does not match the Syntax the local file that corresponds to the path is returned
     * 
     * @param artifactUri Maven artifact URI
     * @return file referenced by the URI
     */
    public File resolve(String artifactUri) {
        for (File bundleDir : mavenRepos) {
            File file = findFile(bundleDir, artifactUri);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private static File findFile(File dir, String name) {
        name = fromMaven(name);
        File theFile = new File(dir, name);

        if (theFile.exists() && !theFile.isDirectory()) {
            return theFile;
        }
        return null;
    }

    

    /**
     * Returns a path for an srtifact.
     * Input: path (no ':') returns path
     * Input:  converts to default repo location path
     * type and classifier are optional.
     *
     *
     * @param name input artifact info
     * @return path as supplied or a default maven repo path
     */
    private static String fromMaven(String name) {
        Matcher m = mvnPattern.matcher(name);
        if (!m.matches()) {
            return name;
        }
        StringBuilder b = new StringBuilder();
        b.append(m.group(1));
        for (int i = 0; i < b.length(); i++) {
            if (b.charAt(i) == '.') {
                b.setCharAt(i, '/');
            }
        }
        b.append("/");//groupId
        String artifactId = m.group(2);
        String version = m.group(3);
        String extension = m.group(5);
        String classifier = m.group(7);
        b.append(artifactId).append("/");//artifactId
        b.append(version).append("/");//version
        b.append(artifactId).append("-").append(version);
        if (present(classifier)) {
            b.append("-").append(classifier);
        } else {
            if (present(extension)) {
                b.append(".").append(extension);
            } else {
                b.append(".jar");
            }
        }
        return b.toString();
    }

    private static boolean present(String part) {
        return part != null && !part.isEmpty();
    }
}
