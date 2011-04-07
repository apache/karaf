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


package org.apache.karaf.tooling.features;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Methods to convert between
 * pax mvn format: mvn-uri := 'mvn:' [ repository-url '!' ] group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]
 * aether coordinate format: <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
 * and repository paths
 *
 * N.B. we do not handle repository-url in mvn urls.
 * N.B. version is required in mvn urls.
 *
 * @version $Rev:$ $Date:$
 */
public class MvnUrlUtil {

    private static final DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
    private static final Pattern aetherPatterh = Pattern.compile( "([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)" );
    private static final Pattern mvnPattern = Pattern.compile( "mvn:([^/ ]+)/([^/ ]+)/([^/ ]*)(/([^/ ]+)(/([^/ ]+))?)?" );

    static String mvnToAether(String name) {
        Matcher m = mvnPattern.matcher(name);
        if (!m.matches()) {
            return name;
        }
        StringBuilder b = new StringBuilder();
        b.append(m.group(1)).append(":");//groupId
        b.append(m.group(2)).append(":");//artifactId
        String extension = m.group(5);
        String classifier = m.group(7);
        if (present(classifier)) {
            if (present(extension)) {
                b.append(extension).append(":");
            } else {
                b.append("jar:");
            }
            b.append(classifier).append(":");
        } else {
            if (present(extension) && !"jar".equals(extension)) {
                b.append(extension).append(":");
            }
        }
        b.append(m.group(3));
        return b.toString();
    }

    private static boolean present(String part) {
        return part != null && !part.isEmpty();
    }

    static String aetherToMvn(String name) {
        Matcher m = aetherPatterh.matcher(name);
        if (!m.matches()) {
            return name;
        }
        StringBuilder b = new StringBuilder("mvn:");
        b.append(m.group(1)).append("/");//groupId
        b.append(m.group(2)).append("/");//artifactId
        b.append(m.group(7));//version
        String extension = m.group(4);
        String classifier = m.group(6);
        if (present(classifier)) {
            if (present(extension)) {
                b.append("/").append(extension);
            } else {
                b.append("/jar");
            }
            b.append("/").append(classifier);
        } else if (present(extension)) {
            b.append("/").append(extension);
        }

        return b.toString();
    }

    /**
     * similar to a Main class method
     * Returns a path for an srtifact.
     * Input: path (no ':') returns path
     * Input: mvn:<groupId>/<artifactId>/<version>/<type>/<classifier> converts to default repo location path
     * Input:  <groupId>:<artifactId>:<type>:<classifier>:<version>:<type>:<classifier> converts to default repo location path
     * type and classifier are optional.
     *
     *
     * @param name input artifact info
     * @return path as supplied or a default maven repo path
     */
    static String pathFromMaven(String name) {
        if (name.indexOf(':') == -1) {
            return name;
        }
        name = mvnToAether(name);
        return pathFromAether(name);
    }

    static String pathFromAether(String name) {
        DefaultArtifact artifact = new DefaultArtifact(name);
        Artifact mavenArtifact = RepositoryUtils.toArtifact(artifact);
        return layout.pathOf(mavenArtifact);
    }

    static String artifactToMvn(Artifact artifact) {
        return  artifactToMvn(RepositoryUtils.toArtifact(artifact));
    }

    static String artifactToMvn(org.sonatype.aether.artifact.Artifact artifact) {
        String bundleName;
        if (artifact.getExtension().equals("jar") && isEmpty(artifact.getClassifier())) {
            bundleName = String.format("mvn:%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        } else {
            if (isEmpty(artifact.getClassifier())) {
                bundleName = String.format("mvn:%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getExtension());
            } else {
                bundleName = String.format("mvn:%s/%s/%s/%s/%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getExtension(), artifact.getClassifier());
            }
        }
        return bundleName;
    }

    private static boolean isEmpty(String classifier) {
        return classifier == null || classifier.length() == 0;
    }

}
