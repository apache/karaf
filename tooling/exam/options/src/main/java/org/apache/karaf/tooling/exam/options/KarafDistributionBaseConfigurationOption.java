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
import org.ops4j.pax.exam.options.MavenUrlReference;

import java.io.File;

public class KarafDistributionBaseConfigurationOption implements Option {

    protected String frameworkUrl;
    protected MavenUrlReference frameworkUrlReference;
    protected String name;
    protected String karafVersion;
    protected File unpackDirectory;
    protected boolean useDeployFolder = true;

    public KarafDistributionBaseConfigurationOption() {
        frameworkUrl = null;
        frameworkUrlReference = null;
        name = null;
        karafVersion = null;
    }

    public KarafDistributionBaseConfigurationOption(String frameworkUrl, String name, String karafVersion) {
        this.frameworkUrl = frameworkUrl;
        frameworkUrlReference = null;
        this.name = name;
        this.karafVersion = karafVersion;
    }

    public KarafDistributionBaseConfigurationOption(MavenUrlReference frameworkUrlReference, String name, String karafVersion) {
        frameworkUrl = null;
        this.frameworkUrlReference = frameworkUrlReference;
        this.name = name;
        this.karafVersion = karafVersion;
    }

    public KarafDistributionBaseConfigurationOption(MavenUrlReference frameworkUrlReference) {
        frameworkUrl = null;
        this.frameworkUrlReference = frameworkUrlReference;
    }

    /**
     * Simply clone the inserted {@link KarafDistributionBaseConfigurationOption}
     *
     * @param base the <code>KarafDistributionBaseConfigurationOption</code> to clone.
     */
    public KarafDistributionBaseConfigurationOption(KarafDistributionBaseConfigurationOption base) {
        frameworkUrl = base.frameworkUrl;
        frameworkUrlReference = base.frameworkUrlReference;
        name = base.name;
        karafVersion = base.karafVersion;
        unpackDirectory = base.unpackDirectory;
        useDeployFolder = base.useDeployFolder;
    }

    /**
     * Set the URL of the framework as a String (for example a file).
     *
     * @param frameworkUrl the framework URL.
     * @return the updated <code>KarafDistributionBaseConfigurationOption</code>.
     */
    public KarafDistributionBaseConfigurationOption frameworkUrl(String frameworkUrl) {
        this.frameworkUrl = frameworkUrl;
        return this;
    }

    /**
     * Set the URL of the framework as a Maven URL reference.
     *
     * @param frameworkUrlReference the framework Maven URL.
     * @return the updated <code>KarafDistributionBaseConfigurationOption</code>.
     */
    public KarafDistributionBaseConfigurationOption frameworkUrl(MavenUrlReference frameworkUrlReference) {
        this.frameworkUrlReference = frameworkUrlReference;
        return this;
    }

    /**
     * Set the name of the framework. This is only used for logging.
     *
     * @param name the framework name.
     * @return the updated <code>KarafDistributionBaseConfigurationOption</code>.
     */
    public KarafDistributionBaseConfigurationOption name(String name) {
        this.name = name;
        return this;
    }

    /**
     * The version of Karaf used by the framework. That one is required since there is the high possibility that
     * configuration is different between various Karaf versions.
     *
     * @param karafVersion the Karaf version to use.
     * @return the updated <code>KarafDistributionBaseConfigurationOption</code>.
     */
    public KarafDistributionBaseConfigurationOption karafVersion(String karafVersion) {
        this.karafVersion = karafVersion;
        return this;
    }

    /**
     * Define the unpack directory for the Karaf distribution. In this directory a UUID named directory will be
     * created for each environment.
     *
     * @param unpackDirectory the unpack directory location.
     * @return the updated <code>KarafDistributionBaseConfigurationOption</code>.
     */
    public KarafDistributionBaseConfigurationOption unpackDirectory(File unpackDirectory) {
        this.unpackDirectory = unpackDirectory;
        return this;
    }

    /**
     * By default, the framework simply copies all referenced artifacts (via PaxExam DistributionOption) to the
     * deploy folder of the Karaf (based) distribution. If you don't have such a folder (for any reason) you can set
     * this option to false. PaxExam Karaf will then try to add those deployment URLs directly to a feature XML instead
     * of copying those files to the deploy folder.
     *
     * @param useDeployFolder flag defining if we have to use the deploy folder (true) or not (false).
     * @return the updated <code>KarafDistributionBaseConfigurationOption</code>.
     */
    public KarafDistributionBaseConfigurationOption useDeployFolder(boolean useDeployFolder) {
        this.useDeployFolder = useDeployFolder;
        return this;
    }

    public String getFrameworkUrl() {
        if (frameworkUrl == null && frameworkUrlReference == null) {
            throw new IllegalStateException("Either frameworkUrl or frameworkUrlReference have to be set.");
        }
        return frameworkUrl != null ? frameworkUrl : frameworkUrlReference.getURL();
    }

    public String getName() {
        return name;
    }

    public String getKarafVersion() {
        return karafVersion;
    }

    public File getUnpackDirectory() {
        return unpackDirectory;
    }

    public boolean isUseDeployFolder() {
        return useDeployFolder;
    }

}
