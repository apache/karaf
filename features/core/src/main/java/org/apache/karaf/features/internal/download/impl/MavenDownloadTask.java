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
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.util.maven.Parser;
import org.ops4j.pax.url.mvn.MavenResolver;

public class MavenDownloadTask extends AbstractRetryableDownloadTask {

    private final MavenResolver resolver;

    public MavenDownloadTask(ScheduledExecutorService executor, MavenResolver resolver, String url) {
        super(executor, url);
        this.resolver = resolver;
    }

    @Override
    public String getUrl() {
        try {
            // This ensures the version of the artifact is resolved in the returned url
            return Parser.pathToMaven(Parser.pathFromMaven(url, getFile().toString()));
        } catch (IOException e) {
            return super.getUrl();
        }
    }

    @Override
    protected File download(Exception previousException) throws Exception {
                
        try {
            return resolver.resolve(url, previousException);
        } catch (Exception ex) {
            //try again with removing timestamp from snapshot 
            return resolver.resolve(Parser.pathToMaven(Parser.pathFromMaven(url)), previousException);
        }
    }

    /**
     * Maven artifact may be looked up in several repositories. Only if exception for <strong>each</strong>
     * repository is not retryable, we won't retry.
     * @param e
     * @return
     */
    @Override
    protected Retry isRetryable(IOException e) {
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
