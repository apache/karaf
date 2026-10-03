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

import org.apache.karaf.features.internal.download.impl.AbstractRetryableDownloadTask.Retry;

/**
 * Resolves <code>mvn:</code> URLs for the {@link MavenDownloadManager}, whatever the Maven resolver actually
 * available (pax-url-aether or the Karaf URL service).
 */
public interface MavenArtifactResolver {

    /**
     * Resolve a Maven artifact to a local file.
     *
     * @param url the <code>mvn:</code> URL of the artifact
     * @param previousException the exception thrown by the previous attempt (if any), which may be used as a hint
     * @return the resolved file
     * @throws IOException if the artifact can't be resolved
     */
    File resolve(String url, Exception previousException) throws IOException;

    /**
     * @param e the exception thrown by {@link #resolve(String, Exception)}
     * @return what kind of retry may be attempted
     */
    Retry isRetryable(IOException e);

}
