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

/**
 * Represents a Docker image search result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageSearch {

    @JsonProperty("name")
    private String name;

    @JsonProperty("star_count")
    private int starCount;

    @JsonProperty("is_official")
    private boolean official;

    @JsonProperty("is_automated")
    private boolean automated;

    @JsonProperty("description")
    private String description;

    public String getName() {
        return name;
    }

    public int getStarCount() {
        return starCount;
    }

    public boolean isOfficial() {
        return official;
    }

    public boolean isAutomated() {
        return automated;
    }

    public String getDescription() {
        return description;
    }

}
