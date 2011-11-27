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

package org.apache.karaf.tooling.exam.options;


/**
 * This option allows to extend configurations in each configuration file based on the karaf.home location. The value
 * extends the current value (e.g. a=b to a=a,b) instead of replacing it. If there is no current value it is added.
 *
 * If you would like to have add or replace functionality please use the
 * {@link KarafDistributionConfigurationFilePutOption} instead.
 */
public class KarafDistributionConfigurationFileExtendOption extends KarafDistributionConfigurationFileOption {

    public KarafDistributionConfigurationFileExtendOption(String configurationFilePath, String key, String value) {
        super(configurationFilePath, key, value);
    }

    public KarafDistributionConfigurationFileExtendOption(ConfigurationPointer pointer, String value) {
        super(pointer, value);
    }

}
