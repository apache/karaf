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
package org.apache.karaf.profile.assembly;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.felix.utils.resource.ResourceImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Fake bundle revision implementation for resolution simulations without OSGi.
 */
public class FakeBundleRevision extends ResourceImpl implements BundleRevision, BundleStartLevel {

    private final Bundle bundle;
    private int startLevel;

    public FakeBundleRevision(final Hashtable<String, String> headers, final String location, final long bundleId) throws BundleException {
        ResourceBuilder.build(this, location, headers);
        this.bundle = (Bundle) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Bundle.class},
                new BundleRevisionInvocationHandler(headers, location, bundleId));
    }

    @Override
    public int getStartLevel() {
        return startLevel;
    }

    @Override
    public void setStartLevel(int startLevel) {
        this.startLevel = startLevel;
    }

    @Override
    public boolean isPersistentlyStarted() {
        return true;
    }

    @Override
    public boolean isActivationPolicyUsed() {
        return false;
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
    public List<BundleCapability> getDeclaredCapabilities(String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<BundleRequirement> getDeclaredRequirements(String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BundleWiring getWiring() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    private class BundleRevisionInvocationHandler implements InvocationHandler {

        private final Hashtable<String, String> headers;
        private final String location;
        private final long bundleId;

        public BundleRevisionInvocationHandler(Hashtable<String, String> headers, String location, long bundleId) {
            this.headers = headers;
            this.location = location;
            this.bundleId = bundleId;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "hashCode":
                    return FakeBundleRevision.this.hashCode();
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return bundle.getSymbolicName() + "/" + bundle.getVersion();
                case "adapt":
                    if (args.length == 1 && args[0] == BundleRevision.class) {
                        return FakeBundleRevision.this;
                    } else if (args.length == 1 && args[0] == BundleStartLevel.class) {
                        return FakeBundleRevision.this;
                    }
                    break;
                case "getHeaders":
                    return headers;
                case "getBundleId":
                    return bundleId;
                case "getLocation":
                    return location;
                case "getSymbolicName":
                    String name = headers.get(Constants.BUNDLE_SYMBOLICNAME);
                    int idx = name.indexOf(';');
                    if (idx > 0) {
                        name = name.substring(0, idx).trim();
                    }
                    return name;
                case "getVersion":
                    return new Version(headers.get(Constants.BUNDLE_VERSION));
                case "getState":
                    return Bundle.ACTIVE;
                case "getLastModified":
                    return 0l;
            }
            return null;
        }
    }
}
