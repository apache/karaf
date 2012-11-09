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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.osgi.framework.Bundle;

public class InstallationState {
    final Set<Bundle> installed = new HashSet<Bundle>();
    final Set<Bundle> bundles = new TreeSet<Bundle>();
    final Map<Long, BundleInfo> bundleInfos = new HashMap<Long, BundleInfo>();
    final Map<Bundle,Integer> bundleStartLevels = new HashMap<Bundle, Integer>();
    final Map<Feature, Set<Long>> features = new HashMap<Feature, Set<Long>>();
}