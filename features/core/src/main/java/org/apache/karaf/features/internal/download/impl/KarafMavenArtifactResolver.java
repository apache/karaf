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
import org.apache.karaf.services.url.MavenResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Resolves the artifacts with the Maven resolver service of the Karaf URL service.
 */
public class KarafMavenArtifactResolver implements MavenArtifactResolver {

    public static final String SERVICE = "org.apache.karaf.services.url.MavenResolver";

    private final BundleContext bundleContext;

    public KarafMavenArtifactResolver(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        // the service is registered again each time the Karaf URL service is configured, so do not hold it
        ServiceReference<MavenResolver> reference = bundleContext.getServiceReference(MavenResolver.class);
        MavenResolver resolver = reference != null ? bundleContext.getService(reference) : null;
        if (resolver == null) {
            throw new IOException("Maven resolver service is not available");
        }
        try {
            return resolver.resolve(url);
        } finally {
            bundleContext.ungetService(reference);
        }
    }

    @Override
    public Retry isRetryable(IOException e) {
        // the Karaf Maven resolver already retries to download the artifact
        return Retry.NO_RETRY;
    }

}
