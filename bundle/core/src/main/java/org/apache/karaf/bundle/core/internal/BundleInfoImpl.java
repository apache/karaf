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
package org.apache.karaf.bundle.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleState;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;

public class BundleInfoImpl implements BundleInfo {

    private int startLevel;
    private String name;
    private String symbolicName;
    private String updateLocation;
    private String version;
    private String revisions;
    private long bundleId;
    private BundleState state;
    private boolean isFragment;
    private List<Bundle> fragments;
    private List<Bundle> fragmentHosts;
    
    private static Map<Integer, BundleState> bundleStateMap;
    
    static {
        bundleStateMap = new HashMap<>();
        bundleStateMap.put(Bundle.ACTIVE, BundleState.Active);
        bundleStateMap.put(Bundle.INSTALLED, BundleState.Installed);
        bundleStateMap.put(Bundle.RESOLVED, BundleState.Resolved);
        bundleStateMap.put(Bundle.STARTING, BundleState.Starting);
        bundleStateMap.put(Bundle.STOPPING, BundleState.Stopping);
    }

    public BundleInfoImpl(Bundle bundle, BundleState extState) {
        BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
        this.startLevel = bsl.getStartLevel();
        this.name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
        this.symbolicName = bundle.getSymbolicName();
        String locationFromHeader = bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
        this.updateLocation = locationFromHeader != null ? locationFromHeader : bundle.getLocation();
        this.version = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
        this.revisions = populateRevisions(bundle);
        this.bundleId = bundle.getBundleId();
        this.state = (extState != BundleState.Unknown) ? extState : getBundleState(bundle);
        populateFragementInfos(bundle);
    }

    private void populateFragementInfos(Bundle bundle) {
        this.isFragment = bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
        this.fragments = new ArrayList<>();
        this.fragmentHosts = new ArrayList<>();
        BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
        if (revisions == null) {
            return;
        }
        for (BundleRevision revision : revisions.getRevisions()) {
            if (revision.getWiring() != null) {
                getFragments(revision);
                getFragmentHosts(revision);
            }
        }
    }
    
    private String populateRevisions(Bundle bundle) {
        BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
        if (revisions == null) {
            return "";
        }
        StringBuilder ret = new StringBuilder();
        for (BundleRevision revision : revisions.getRevisions()) {
            ret.append("[").append(revision).append("]").append(" ");
        }
        return ret.toString();
    }

    private void getFragments(BundleRevision revision) {
        List<BundleWire> wires = revision.getWiring().getProvidedWires(BundleRevision.HOST_NAMESPACE);
        if (wires != null) {
            for (BundleWire w : wires) {
                Bundle b = w.getRequirerWiring().getBundle();
                this.fragments.add(b);
            }
        }
    }

    private void getFragmentHosts(BundleRevision revision) {
        List<BundleWire> wires = revision.getWiring().getRequiredWires(BundleRevision.HOST_NAMESPACE);
        if (wires != null) {
            for (BundleWire w : wires) {
                Bundle b = w.getProviderWiring().getBundle();
                if (b != null) {
                    this.fragmentHosts.add(b);
                }
            }
        }
    }

    private BundleState getBundleState(Bundle bundle) {
        BundleState state = bundleStateMap.get(bundle.getState());
        return state == null ? BundleState.Unknown : state;
    }

    @Override
    public long getBundleId() {
        return this.bundleId;
    }

    @Override
    public String getSymbolicName() {
        return this.symbolicName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUpdateLocation() {
        return this.updateLocation;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public BundleState getState() {
        return this.state;
    }
    
    @Override
    public int getStartLevel() {
        return this.startLevel;
    }

    @Override
    public boolean isFragment() {
        return this.isFragment;
    }
    
    @Override
    public List<Bundle> getFragments() {
        return this.fragments;
    }

    @Override
    public List<Bundle> getFragmentHosts() {
        return this.fragmentHosts;
    }

    @Override
    public String getRevisions() {
        return this.revisions;
    }

}
