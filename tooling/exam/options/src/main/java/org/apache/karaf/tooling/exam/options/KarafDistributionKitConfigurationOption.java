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

import org.ops4j.pax.exam.options.MavenUrlReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Option describing the Karaf distribution to use. This option uses the specified scripts to run the environment
 * depending on the platform. If a platform is defined and run on another platform, it is ignored.
 */
public class KarafDistributionKitConfigurationOption extends KarafDistributionBaseConfigurationOption {

    public enum Platform {
        WINDOWS, NIX
    }

    private Platform platform;
    private List<String> makeExec = new ArrayList<String>();
    private String exec;

    public KarafDistributionKitConfigurationOption(KarafDistributionBaseConfigurationOption base, Platform platform) {
        super(base);
        setPlatform(platform);
    }

    public KarafDistributionKitConfigurationOption(MavenUrlReference frameworkUrlReference, String name, String karafVersion, Platform platform) {
        super(frameworkUrlReference, name, karafVersion);
        setPlatform(platform);
    }

    public KarafDistributionKitConfigurationOption(MavenUrlReference frameworkUrlReference, Platform platform) {
        super(frameworkUrlReference);
        setPlatform(platform);
    }

    public KarafDistributionKitConfigurationOption(String frameworkUrl, String name, String karafVersion, Platform platform) {
        super(frameworkUrl, name, karafVersion);
        setPlatform(platform);
    }

    private void setPlatform(Platform platform) {
        this.platform = platform;
        if (platform.equals(Platform.WINDOWS)) {
            exec = "bin\\karaf.bat";
        } else {
            exec = "bin/karaf";
        }
    }

    public KarafDistributionKitConfigurationOption filesToMakeExecutable(String... platformRelativeFilePath) {
        for (String platformRelativePath : platformRelativeFilePath) {
            makeExec.add(platformRelativePath);
        }
        return this;
    }

    public KarafDistributionKitConfigurationOption executable(String platformRelativeFilePath) {
        exec = platformRelativeFilePath;
        return this;
    }

    public Platform getPlatform() {
        return platform;
    }

    public List<String> getMakeExec() {
        return makeExec;
    }

    public String getExec() {
        return exec;
    }

}
