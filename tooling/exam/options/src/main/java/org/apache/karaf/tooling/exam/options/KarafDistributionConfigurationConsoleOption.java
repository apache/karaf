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
 * Option to configure the Karaf <code>-Dkaraf.startLocalConsole</code> and <code>-Dkaraf.startRemoteShell</code>
 * options. By default, both are started automatically. If you want to change this behavior, simply add this option
 * to your container configuration.
 */
public class KarafDistributionConfigurationConsoleOption implements Option {

    private Boolean startLocalConsole;
    private Boolean startRemoteShell;

    public KarafDistributionConfigurationConsoleOption(Boolean startLocalConsole, Boolean startRemoteShell) {
        this.startLocalConsole = startLocalConsole;
        this.startRemoteShell = startRemoteShell;
    }

    /**
     * Set the <code>-Dkaraf.startLocalConsole</code> to true.
     *
     * @return the updated <code>KarafDistributionConfigurationConsoleOption</code>.
     */
    public KarafDistributionConfigurationConsoleOption startLocalConsole() {
        startLocalConsole = true;
        return this;
    }

    /**
     * Set the <code>-Dkaraf.startLocalConsole</code> to false.
     *
     * @return the updated <code>KarafDistributionConfigurationConsoleOption</code>.
     */
    public KarafDistributionConfigurationConsoleOption ignoreLocalConsole() {
        startLocalConsole = false;
        return this;
    }

    /**
     * Set the <code>-Dkaraf.startRemoteShell</code> to true.
     *
     * @return the updated <code>KarafDistributionConfigurationConsoleOption</code>.
     */
    public KarafDistributionConfigurationConsoleOption startRemoteShell() {
        startRemoteShell = true;
        return this;
    }

    /**
     * Set the <code>-Dkaraf.startRemoteShell</code> to false.
     *
     * @return the updated <code>KarafDistributionConfigurationConsoleOption</code>.
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
