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

/**
 * Represent a Docker Container.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Container {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Image")
    private String image;

    @JsonProperty("ImageID")
    private String imageId;

    @JsonProperty("Command")
    private String command;

    @JsonProperty("Created")
    private long created;

    @JsonProperty("State")
    private String state;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Ports")
    private List<Port> ports;

    @JsonProperty("SizeRw")
    private long sizeRw;

    @JsonProperty("SizeRootFS")
    private long sizeRootFs;

    @JsonProperty("Names")
    private List<String> names;

    public String getId() {
        return id;
    }

    public String getImage() {
        return image;
    }

    public String getCommand() {
        return command;
    }

    public long getCreated() {
        return created;
    }

    public String getStatus() {
        return status;
    }

    public List<Port> getPorts() {
        return ports;
    }

    public long getSizeRw() {
        return sizeRw;
    }

    public long getSizeRootFs() {
        return sizeRootFs;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getImageId() {
        return imageId;
    }

    public String getState() {
        return state;
    }

}
