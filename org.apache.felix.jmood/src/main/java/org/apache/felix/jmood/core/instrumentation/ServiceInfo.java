/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.jmood.core.instrumentation;

import java.io.Serializable;
import java.util.Hashtable;

public class ServiceInfo implements Serializable{
    private BundleInfo bundle;
    private BundleInfo[] usingBundles;
    private Hashtable properties;
    public ServiceInfo() {
        super();
        // TODO Auto-generated constructor stub
    }
    public BundleInfo getBundle() {
        return bundle;
    }
    protected void setBundle(BundleInfo bundle) {
        this.bundle = bundle;
    }
    public Hashtable getProperties() {
        return properties;
    }
    protected void setProperties(Hashtable properties) {
        this.properties = properties;
    }
    public BundleInfo[] getUsingBundles() {
        return usingBundles;
    }
    protected void setUsingBundles(BundleInfo[] usingBundles) {
        this.usingBundles = usingBundles;
    }
}
