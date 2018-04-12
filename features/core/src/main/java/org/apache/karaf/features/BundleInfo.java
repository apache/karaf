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
package org.apache.karaf.features;

/**
 * A bundle info holds info about a Bundle.
 */
public interface BundleInfo extends Blacklisting {

    String getLocation();

    String getOriginalLocation();

    int getStartLevel();

    boolean isStart();

    boolean isDependency();

    BundleInfo.BundleOverrideMode isOverriden();

    public enum BundleOverrideMode {
        /**
         * No override
         */
        NONE,

        /**
         * Compatibility with <code>${karaf.etc}/overrides.properties</code> - requires access to original and
         * replacement bundle's headers to compare version and symbolic name.
         */
        OSGI,

        /**
         * Simpler option that's just static override - doesn't require accessing and checking the bundle/resource
         * being overriden.
         */
        MAVEN
    }

}
