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

import java.io.File;

/**
 * If you do not want to replace (or extend) values in a file but rather simply want to replace a configuration file
 * "brute force" this option is the one of your choice. It simply removes the original file and replaces it with the one
 * configured here.
 */
public class KarafDistributionConfigurationFileReplacementOption extends KarafDistributionConfigurationFileOption {

    private File source;

    public KarafDistributionConfigurationFileReplacementOption(String configurationFilePath, File source) {
        super(configurationFilePath);
        this.source = source;
    }

    public File getSource() {
        return source;
    }

}
