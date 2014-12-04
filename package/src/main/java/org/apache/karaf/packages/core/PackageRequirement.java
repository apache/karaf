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

import org.osgi.framework.Bundle;

public class PackageRequirement {
    private String filter;
    private boolean optional;
    private Bundle bundle;
    private boolean resolveable;
    private String packageName;
    private String minVersion;
    private String maxVersion;

    public PackageRequirement(String filter, boolean optional, Bundle bundle, boolean resolveable, String packageName, String minVersion, String maxVersion) {
        super();
        this.filter = filter;
        this.optional = optional;
        this.bundle = bundle;
        this.resolveable = resolveable;
        this.packageName = packageName;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
    }
    
    public Bundle getBundle() {
        return bundle;
    }

    public String getFilter() {
        return filter;
    }
    public boolean isOptional() {
        return optional;
    }

    public boolean isResolveable() {
        return resolveable;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getMinVersion() {
        return minVersion;
    }
    
    public String getMaxVersion() {
        return maxVersion;
    }
    
    public String getVersionRange() {
        if (minVersion == null && maxVersion == null) {
            return "";
        }
        return String.format("[%s,%s)", getString(minVersion), getString(maxVersion));
    }

    private String getString(String version) {
        return version == null ? "" : version;
    }
    

    public String toString() {
        return String.format("%s;version=\"[%s,%s)\"", packageName, minVersion, maxVersion); 
    }
}
