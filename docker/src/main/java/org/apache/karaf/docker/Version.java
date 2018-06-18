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
package org.apache.karaf.docker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Version {

    @JsonProperty("Version")
    private String version;

    @JsonProperty("Os")
    private String os;

    @JsonProperty("KernelVersion")
    private String kernelVersion;

    @JsonProperty("GoVersion")
    private String goVersion;

    @JsonProperty("GitCommit")
    private String gitCommit;

    @JsonProperty("Arch")
    private String arch;

    @JsonProperty("ApiVersion")
    private String apiVersion;

    @JsonProperty("BuildTime")
    private String buildTime;

    @JsonProperty("Experimental")
    private String experimental;

    public String getVersion() {
        return version;
    }

    public String getOs() {
        return os;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public String getGoVersion() {
        return goVersion;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public String getArch() {
        return arch;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public String getExperimental() {
        return experimental;
    }

}
