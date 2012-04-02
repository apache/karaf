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
package org.apache.karaf.packages.core;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class PackageVersion {

    private String packageName;
    private Version version;
    private List<Bundle> bundles = new ArrayList<Bundle>();
    
    public PackageVersion(String packageName, Version version) {
        this.packageName = packageName;
        this.version = version;
    }
    
    public String getPackageName() {
        return packageName;
    }

    public Version getVersion() {
        return version;
    }
    
    public void addBundle(Bundle bundle) {
        this.bundles.add(bundle);
    }
    
    public List<Bundle> getBundles() {
        return this.bundles;
    }

}