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
package org.apache.karaf.tooling.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.Parser;

/**
 * {@link MavenResolver} that may look up artifacts inside Maven reactor
 */
public class ReactorMavenResolver implements MavenResolver {

    private final WorkspaceReader reactor;
    private final MavenResolver fallback;

    public ReactorMavenResolver(WorkspaceReader reactor, MavenResolver fallback) {
        this.reactor = reactor;
        this.fallback = fallback;
    }

    private Artifact toArtifact(String url) throws MalformedURLException {
        if (url.startsWith(ServiceConstants.PROTOCOL + ":")) {
            url = url.substring(4);
        }
        Parser parser = new Parser(url);
        return new DefaultArtifact(parser.getGroup(), parser.getArtifact(), parser.getClassifier(),
                parser.getType(), parser.getVersion());
    }

    @Override
    public File resolve(String url) throws IOException {
        Artifact artifact = toArtifact(url);
        File file = reactor.findArtifact(artifact);
        return file == null ? fallback.resolve(url) : file;
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        Artifact artifact = toArtifact(url);
        File file = reactor.findArtifact(artifact);
        return file == null ? fallback.resolve(url, previousException) : file;
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version) throws IOException {
        File file = reactor.findArtifact(new DefaultArtifact(groupId, artifactId, classifier, extension, version));
        return file == null ? fallback.resolve(String.format("mvn:%s/%s/%s/%s/%s", groupId, artifactId, version, extension, classifier)) : file;
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version, Exception previousException) throws IOException {
        File file = reactor.findArtifact(new DefaultArtifact(groupId, artifactId, classifier, extension, version));
        return file == null ? fallback.resolve(String.format("mvn:%s/%s/%s/%s/%s", groupId, artifactId, version, extension, classifier), previousException) : file;
    }

    @Override
    public File resolveMetadata(String groupId, String artifactId, String type, String version) throws IOException {
        return fallback.resolveMetadata(groupId, artifactId, type, version);
    }

    @Override
    public File resolveMetadata(String groupId, String artifactId, String type, String version, Exception previousException) throws IOException {
        return fallback.resolveMetadata(groupId, artifactId, type, version, previousException);
    }

    @Override
    public void upload(String groupId, String artifactId, String classifier, String extension, String version, File artifact) throws IOException {
        fallback.upload(groupId, artifactId, classifier, extension, version, artifact);
    }

    @Override
    public void uploadMetadata(String groupId, String artifactId, String type, String version, File artifact) throws IOException {
        fallback.uploadMetadata(groupId, artifactId, type, version, artifact);
    }

    @Override
    public RetryChance isRetryableException(Exception exception) {
        return fallback.isRetryableException(exception);
    }

    @Override
    public void close() throws IOException {
        fallback.close();
    }

}
