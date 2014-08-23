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
package org.apache.karaf.features.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Feature;

public class ConditionalImpl extends ContentImpl implements Conditional {

    private List<Feature> conditions = new ArrayList<Feature>();

    public List<Feature> getCondition() {
        return conditions;
    }

    public void addCondition(Feature condition) {
        conditions.add(condition);
    }

    public Feature asFeature(String name, String version) {
        String conditionName = name + "-condition-" + getConditionId().replaceAll("[^A-Za-z0-9 ]", "_");
        FeatureImpl f = new FeatureImpl(conditionName, version);
        f.getBundles().addAll(getBundles());
		f.getConfigurations().addAll(getConfigurations());
        f.getConfigurationFiles().addAll(getConfigurationFiles());
        f.getDependencies().addAll(getDependencies());
        return f;
    }

    private String getConditionId() {
        StringBuilder sb = new StringBuilder();
        Iterator<Feature> di = getCondition().iterator();
        while (di.hasNext()) {
            Feature dependency = di.next();
            sb.append(dependency.getName()).append("_").append(dependency.getVersion());
            if (di.hasNext()) {
                sb.append("_");
            }
        }
        return sb.toString();
    }

}
