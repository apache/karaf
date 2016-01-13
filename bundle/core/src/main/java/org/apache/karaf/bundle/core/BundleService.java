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
package org.apache.karaf.bundle.core;

import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRequirement;

public interface BundleService {

    String SYSTEM_BUNDLES_ROLE = "systembundles";
    
    BundleInfo getInfo(Bundle bundle);

    List<Bundle> selectBundles(List<String> ids, boolean defaultAllBundles);

    List<Bundle> selectBundles(String context, List<String> ids, boolean defaultAllBundles);

    Bundle getBundle(String id);

    Bundle getBundle(String context, String id);

    String getDiag(Bundle bundle);
    
    List<BundleRequirement> getUnsatisfiedRequirements(Bundle bundle, String namespace);
    
    Map<String, Bundle> getWiredBundles(Bundle bundle);
    
    boolean isDynamicImport(Bundle bundle);

    void enableDynamicImports(Bundle bundle);

    void disableDynamicImports(Bundle bundle);

    int getSystemBundleThreshold();

    String getStatus(String id);

}
