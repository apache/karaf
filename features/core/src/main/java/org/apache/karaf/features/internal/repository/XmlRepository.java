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
package org.apache.karaf.features.internal.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository conforming to the OSGi Repository specification.
 * The content of the URL can be gzipped.
 */
public class XmlRepository extends org.apache.felix.utils.repository.XmlRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRepository.class);

    protected final boolean ignoreFailures;

    public XmlRepository(String url, long expiration, boolean ignoreFailures) {
        super(url, expiration);
        this.ignoreFailures = ignoreFailures;
    }

    protected void checkAndLoadCache() {
        try {
            super.checkAndLoadCache();
        } catch (Exception e) {
            if (ignoreFailures) {
                LOGGER.warn("Ignoring failure: " + e.getMessage(), e);
            } else {
                throw e;
            }
        }
    }

}
