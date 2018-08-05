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

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Info {

    @JsonProperty("Containers")
    private int containers;

    @JsonProperty("Debug")
    private boolean debug;

    @JsonProperty("Driver")
    private String driver;

    @JsonProperty("DriverStatus")
    private List<List<String>> driverStatus;

    @JsonProperty("ExecutionDriver")
    private String executionDriver;

    @JsonProperty("IPv4Forwarding")
    private boolean ipv4Forwarding;

    @JsonProperty("Images")
    private int images;

    @JsonProperty("IndexServerAddress")
    private String indexServerAddress;

    @JsonProperty("InitPath")
    private String initPath;

    @JsonProperty("InitSha1")
    private String initSha1;

    @JsonProperty("KernelVersion")
    private String kernelVersion;

    @JsonProperty("MemoryLimit")
    private boolean memoryLimit;

    @JsonProperty("NEventsListener")
    private boolean nEventsListener;

    @JsonProperty("NFd")
    private int nfd;

    @JsonProperty("NGoroutines")
    private int ngoroutines;

    @JsonProperty("SwapLimit")
    private boolean swapLimit;

    public int getContainers() {
        return containers;
    }

    public void setContainers(int containers) {
        this.containers = containers;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public List<List<String>> getDriverStatus() {
        return driverStatus;
    }

    public void setDriverStatus(List<List<String>> driverStatus) {
        this.driverStatus = driverStatus;
    }

    public String getExecutionDriver() {
        return executionDriver;
    }

    public void setExecutionDriver(String executionDriver) {
        this.executionDriver = executionDriver;
    }

    public boolean isIpv4Forwarding() {
        return ipv4Forwarding;
    }

    public void setIpv4Forwarding(boolean ipv4Forwarding) {
        this.ipv4Forwarding = ipv4Forwarding;
    }

    public int getImages() {
        return images;
    }

    public void setImages(int images) {
        this.images = images;
    }

    public String getIndexServerAddress() {
        return indexServerAddress;
    }

    public void setIndexServerAddress(String indexServerAddress) {
        this.indexServerAddress = indexServerAddress;
    }

    public String getInitPath() {
        return initPath;
    }

    public void setInitPath(String initPath) {
        this.initPath = initPath;
    }

    public String getInitSha1() {
        return initSha1;
    }

    public void setInitSha1(String initSha1) {
        this.initSha1 = initSha1;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public boolean isnEventsListener() {
        return nEventsListener;
    }

    public void setnEventsListener(boolean nEventsListener) {
        this.nEventsListener = nEventsListener;
    }

    public int getNfd() {
        return nfd;
    }

    public void setNfd(int nfd) {
        this.nfd = nfd;
    }

    public int getNgoroutines() {
        return ngoroutines;
    }

    public void setNgoroutines(int ngoroutines) {
        this.ngoroutines = ngoroutines;
    }

    public boolean isMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(boolean memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public boolean isSwapLimit() {
        return swapLimit;
    }

    public void setSwapLimit(boolean swapLimit) {
        this.swapLimit = swapLimit;
    }

}
