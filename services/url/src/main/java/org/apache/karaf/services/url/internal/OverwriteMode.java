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

/**
 * Strategy applied by the {@code wrap:} protocol when the wrapped jar is already a bundle.
 */
public enum OverwriteMode {

    /** Keep the existing manifest. */
    KEEP,

    /** Merge the wrapping instructions with the existing manifest headers. */
    MERGE,

    /** Ignore the existing manifest and generate a new one. */
    FULL;

    /**
     * @param value the value of the {@code overwrite} instruction (can be {@code null})
     * @return the corresponding mode, {@link #KEEP} if the value is missing or unknown
     */
    public static OverwriteMode parse(String value) {
        if (value != null) {
            try {
                return valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // fall back to the default mode
            }
        }
        return KEEP;
    }

}
