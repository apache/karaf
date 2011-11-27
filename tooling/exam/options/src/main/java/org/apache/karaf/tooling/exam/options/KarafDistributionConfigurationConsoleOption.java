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

import org.ops4j.pax.exam.Option;

/**
 * Option to configure the Karaf -Dkaraf.startLocalConsole and -Dkaraf.startRemoteShell options. Per default both are
 * started automatically. If you like to change this behavior simply add this option to your container configuration.
 */
public class KarafDistributionConfigurationConsoleOption implements Option {

    private Boolean startLocalConsole;
    private Boolean startRemoteShell;

    public KarafDistributionConfigurationConsoleOption(Boolean startLocalConsole, Boolean startRemoteShell) {
        this.startLocalConsole = startLocalConsole;
        this.startRemoteShell = startRemoteShell;
    }

    /**
     * Sets the -Dkaraf.startLocalConsole to true
     */
    public KarafDistributionConfigurationConsoleOption startLocalConsole() {
        startLocalConsole = true;
        return this;
    }

    /**
     * Sets the -Dkaraf.startLocalConsole to false
     */
    public KarafDistributionConfigurationConsoleOption ignoreLocalConsole() {
        startLocalConsole = false;
        return this;
    }

    /**
     * Sets the -Dkaraf.startRemoteShell to true
     */
    public KarafDistributionConfigurationConsoleOption startRemoteShell() {
        startRemoteShell = true;
        return this;
    }

    /**
     * Sets the -Dkaraf.startRemoteShell to false
     */
    public KarafDistributionConfigurationConsoleOption ignoreRemoteShell() {
        startRemoteShell = false;
        return this;
    }

    public Boolean getStartLocalConsole() {
        return startLocalConsole;
    }

    public Boolean getStartRemoteShell() {
        return startRemoteShell;
    }

}
