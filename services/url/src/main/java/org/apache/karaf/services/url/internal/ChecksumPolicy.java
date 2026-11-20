/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.url.internal;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Maven checksum policy: what to do with a download which doesn't match its checksum, or has none.
 */
enum ChecksumPolicy {

    /** Reject the download. */
    FAIL,

    /** Use the download, with a warning. */
    WARN,

    /** Don't verify the download. */
    IGNORE;

    private static final Logger LOG = LoggerFactory.getLogger(ChecksumPolicy.class);

    /**
     * @param policy the policy, case insensitive
     * @return the policy, <code>null</code> when it is not set, {@link #WARN} (as Maven) when it is invalid
     */
    static ChecksumPolicy parse(String policy) {
        if (policy == null || policy.trim().isEmpty()) {
            return null;
        }
        try {
            return valueOf(policy.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid checksum policy {}, using warn", policy);
            return WARN;
        }
    }

}
