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
package org.apache.karaf.tooling.utils;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.ops4j.pax.url.mvn.MavenResolver;

public class InternalMavenResolver implements MavenResolver {

    private final DependencyHelper dependencyHelper;
    private final Log log;

    public InternalMavenResolver(DependencyHelper dependencyHelper, Log log) {
        this.dependencyHelper = dependencyHelper;
        this.log = log;
    }

    @Override
    public File resolve(String url) throws IOException {
        try {
            return dependencyHelper.resolveById(url, log);
        } catch (MojoFailureException e) {
            throw new IOException(e);
        }
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File resolveMetadata(String groupId, String artifactId, String type, String version) throws IOException {
        return null;
    }

    @Override
    public void upload(String groupId, String artifactId, String classifier, String extension, String version, File artifact) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uploadMetadata(String groupId, String artifactId, String type, String version, File artifact) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
    }
}
