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
package org.apache.karaf.features.internal.service;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public abstract class StaticInstallSupport implements BundleInstallSupport {

    protected boolean failOnUninstall = true;
    protected boolean failOnUpdate = true;

    @Override
    public void print(String message, boolean verbose) {
    }

    @Override
    public void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
    }

    @Override
    public void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException {
        System.err.println("Update bundle is not supported in the static installer");
        if (failOnUpdate) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void uninstall(Bundle bundle) throws BundleException {
        System.err.println("Uninstall bundle is not supported in the static installer");
        if (failOnUninstall) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void startBundle(Bundle bundle) throws BundleException {
    }

    @Override
    public void stopBundle(Bundle bundle, int options) throws BundleException {
    }

    @Override
    public void setBundleStartLevel(Bundle bundle, int startLevel) {
    }

    @Override
    public void resolveBundles(Set<Bundle> bundles, Map<Resource, List<Wire>> wiring,
                               Map<Resource, Bundle> resToBnd) {
    }

    @Override
    public void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies,
                               Map<String, Set<Long>> bundles)
        throws BundleException, InvalidSyntaxException {
    }

    @Override
    public void saveDigraph() {
    }

    @Override
    public RegionDigraph getDiGraphCopy() throws BundleException {
        return null;
    }

    @Override
    public File getDataFile(String name) {
        return null;
    }

    @Override
    public FrameworkInfo getInfo() {
        return new FrameworkInfo();
    }

    @Override
    public void unregister() {
    }

}
