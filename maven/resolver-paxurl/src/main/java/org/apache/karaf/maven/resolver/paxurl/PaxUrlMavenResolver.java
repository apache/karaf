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
package org.apache.karaf.maven.resolver.paxurl;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.karaf.features.spi.MavenResolver;

/**
 * Adapts a pax-url-aether resolver to the Karaf {@link MavenResolver} SPI.
 *
 * <p>This bundle is the only place in Karaf that links against <code>org.ops4j.pax.url.mvn</code>.</p>
 */
public class PaxUrlMavenResolver implements MavenResolver {

    private final org.ops4j.pax.url.mvn.MavenResolver delegate;

    public PaxUrlMavenResolver(org.ops4j.pax.url.mvn.MavenResolver delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public File resolve(String url) throws IOException {
        return delegate.resolve(url);
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        return delegate.resolve(url, previousException);
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version)
            throws IOException {
        return delegate.resolve(groupId, artifactId, classifier, extension, version);
    }

    @Override
    public RetryChance isRetryableException(Exception exception) {
        switch (delegate.isRetryableException(exception)) {
            case NEVER:
                return RetryChance.NEVER;
            case LOW:
                return RetryChance.LOW;
            case HIGH:
                return RetryChance.HIGH;
            case UNKNOWN:
            default:
                return RetryChance.UNKNOWN;
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

}
