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
package org.apache.karaf.features.internal.download.impl;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Objects;

import org.apache.karaf.features.internal.download.impl.AbstractRetryableDownloadTask.Retry;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;

/**
 * Resolves the artifacts with pax-url-aether.
 */
public class PaxMavenArtifactResolver implements MavenArtifactResolver {

    public static final String PID = "org.ops4j.pax.url.mvn";

    private final MavenResolver resolver;

    public PaxMavenArtifactResolver(MavenResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * @param config the configuration of the {@link #PID} PID
     * @return a resolver using a new pax-url-aether resolver
     */
    public static MavenArtifactResolver create(Dictionary<String, String> config) {
        return new PaxMavenArtifactResolver(MavenResolvers.createMavenResolver(config, PID));
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        return resolver.resolve(url, previousException);
    }

    /**
     * Maven artifact may be looked up in several repositories. Only if exception for <strong>each</strong>
     * repository is not retryable, we won't retry.
     */
    @Override
    public Retry isRetryable(IOException e) {
        // convert pax-url-aether "retry" to features.core "retry" concept
        switch (resolver.isRetryableException(e)) {
            case NEVER:
                return Retry.NO_RETRY;
            case LOW:
            case HIGH:
                // no need to repeat many times
                return Retry.QUICK_RETRY;
            case UNKNOWN:
            default:
                return Retry.DEFAULT_RETRY;
        }
    }

}
