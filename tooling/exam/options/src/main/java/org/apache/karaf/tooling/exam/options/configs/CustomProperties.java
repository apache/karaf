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
package org.apache.karaf.tooling.exam.options.configs;

import org.apache.karaf.tooling.exam.options.ConfigurationPointer;

/**
 * Pre configured property file pointers to the most commonly used properties in /etc/config.properties and
 * /etc/custom.properties.
 */
public interface CustomProperties {

    static final String FILE_PATH = "etc/config.properties";

    /**
     * Possible values here are felix or equinox
     */
    static final ConfigurationPointer KARAF_FRAMEWORK = new CustomPropertiesPointer("karaf.framework");

    static final ConfigurationPointer SYSTEM_PACKAGES_EXTRA = new CustomPropertiesPointer(
            "org.osgi.framework.system.packages.extra");

    static final ConfigurationPointer BOOTDELEGATION = new CustomPropertiesPointer("org.osgi.framework.bootdelegation");

    static class CustomPropertiesPointer extends ConfigurationPointer {

        public CustomPropertiesPointer(String key) {
            super(FILE_PATH, key);
        }

    }

}
