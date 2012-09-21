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
package org.apache.karaf.features.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;

/**
 * The repository implementation.
 */
public class RepositoryImpl implements Repository {

    private URI uri;
    private boolean valid;
    private Features features;

    public RepositoryImpl(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return features.getName();
    }

    public URI getURI() {
        return uri;
    }

    public URI[] getRepositories() throws Exception {
        load();
        URI[] result = new URI[features.getRepository().size()];
        for (int i = 0; i < features.getRepository().size(); i++) {
            String uri = features.getRepository().get(i);
            uri = uri.trim();
            result[i] = URI.create(uri);
        }
        return result;
    }

    public org.apache.karaf.features.Feature[] getFeatures() throws Exception {
        load();
        return features.getFeature().toArray(new org.apache.karaf.features.Feature[0]);
    }


    public void load() throws IOException {
        if (features == null) {
            try {
                InputStream inputStream = uri.toURL().openStream();
                try {
                    features = JaxbUtil.unmarshal(inputStream, false);
                } finally {
                    inputStream.close();
                }
                valid = true;
            } catch (IllegalArgumentException e) {
                throw (IOException) new IOException(e.getMessage() + " : " + uri).initCause(e);
            } catch (Exception e) {
                throw (IOException) new IOException(e.getMessage() + " : " + uri).initCause(e);
            }
        }
    }

    public boolean isValid() {
        return this.valid;
    }

}

