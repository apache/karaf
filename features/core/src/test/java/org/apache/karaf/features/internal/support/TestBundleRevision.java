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
package org.apache.karaf.features.internal.support;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class TestBundleRevision implements BundleRevision {

    private final TestBundle bundle;

    public TestBundleRevision(TestBundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public String getSymbolicName() {
        return bundle.getSymbolicName();
    }

    @Override
    public Version getVersion() {
        return bundle.getVersion();
    }

    @Override
    public List<BundleCapability> getDeclaredCapabilities(String s) {
        return null;
    }

    @Override
    public List<BundleRequirement> getDeclaredRequirements(String s) {
        return null;
    }

    @Override
    public int getTypes() {
        return 0;
    }

    @Override
    public BundleWiring getWiring() {
        return null;
    }

    @Override
    public List<Capability> getCapabilities(String s) {
        return bundle.getCapabilities(null).stream().filter(c -> !c.getNamespace().equals("osgi.content"))
                .map(c -> new CapabilityImpl(this, c.getNamespace(), c.getDirectives(), c.getAttributes()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Requirement> getRequirements(String s) {
        return bundle.getRequirements(s);
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }
}
