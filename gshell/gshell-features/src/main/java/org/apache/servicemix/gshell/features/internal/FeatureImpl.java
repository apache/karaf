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
package org.apache.servicemix.gshell.features.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicemix.gshell.features.Feature;

/**
 * A feature
 */
public class FeatureImpl implements Feature {

    private String name;
    private List<String> bundles = new ArrayList<String>();

    public FeatureImpl(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String[] getBundles() {
        return bundles.toArray(new String[bundles.size()]);
    }

    public void addBundle(String bundle) {
        bundles.add(bundle);
    }
}
