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
package org.apache.karaf.features;

import java.util.EventObject;

public class FeatureEvent extends EventObject {

    public enum EventType {
        FeatureInstalled,
        FeatureUninstalled
    }

    private final EventType type;
    private final Feature feature;
    private final String region;
    private final boolean replay;

    public FeatureEvent(EventType type, Feature feature, String region, boolean replay) {
        super(feature);
        this.type = type;
        this.feature = feature;
        this.region = region;
        this.replay = replay;
    }

    public EventType getType() {
        return type;
    }

    public Feature getFeature() {
        return feature;
    }

    public String getRegion() {
        return region;
    }

    public boolean isReplay() {
        return replay;
    }
}
