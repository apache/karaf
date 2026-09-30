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
package org.apache.karaf.features.spi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Resolves <code>mvn:</code> URIs to local files.
 *
 * <p>This is the only contract the features service requires from a Maven artifact
 * provider. Implementations are looked up through {@link MavenResolvers}.</p>
 */
public interface MavenResolver extends Closeable {

    /**
     * How likely a failed resolution is to succeed when retried.
     */
    enum RetryChance {
        /** Retrying will never help. */
        NEVER,
        /** Retrying may help, but is unlikely to. */
        LOW,
        /** Retrying is likely to help. */
        HIGH,
        /** Not enough information to tell. */
        UNKNOWN
    }

    /**
     * Resolve a <code>mvn:</code> URI to a local file.
     *
     * @param url the URI to resolve.
     * @return the resolved file.
     * @throws IOException if the artifact can't be resolved.
     */
    File resolve(String url) throws IOException;

    /**
     * Resolve a <code>mvn:</code> URI to a local file, carrying over the failure of a
     * previous attempt so the implementation can adjust its behaviour (repository
     * ordering, update policies, ...).
     *
     * @param url the URI to resolve.
     * @param previousException the exception thrown by the previous attempt, or <code>null</code>.
     * @return the resolved file.
     * @throws IOException if the artifact can't be resolved.
     */
    File resolve(String url, Exception previousException) throws IOException;

    /**
     * Resolve a Maven artifact from its coordinates.
     *
     * <p>The default implementation builds the equivalent <code>mvn:</code> URI and delegates to
     * {@link #resolve(String)}.</p>
     *
     * @param groupId the group id.
     * @param artifactId the artifact id.
     * @param classifier the classifier, or <code>null</code>/empty for none.
     * @param extension the extension (packaging/type), or <code>null</code>/empty for the default.
     * @param version the version.
     * @return the resolved file.
     * @throws IOException if the artifact can't be resolved.
     */
    default File resolve(String groupId, String artifactId, String classifier, String extension, String version)
            throws IOException {
        StringBuilder uri = new StringBuilder("mvn:")
                .append(groupId).append('/').append(artifactId).append('/').append(version);
        boolean hasClassifier = classifier != null && !classifier.isEmpty();
        if (hasClassifier || (extension != null && !extension.isEmpty())) {
            uri.append('/').append(extension == null ? "" : extension);
        }
        if (hasClassifier) {
            uri.append('/').append(classifier);
        }
        return resolve(uri.toString());
    }

    /**
     * Tell whether a failed resolution is worth retrying.
     *
     * @param exception the exception thrown by {@link #resolve(String, Exception)}.
     * @return the chance that a retry succeeds.
     */
    default RetryChance isRetryableException(Exception exception) {
        return RetryChance.UNKNOWN;
    }

    @Override
    default void close() throws IOException {
    }

}
