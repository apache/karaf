package org.apache.karaf.features.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.osgi.framework.Bundle;

class InstallationState {
    final Set<Bundle> installed = new HashSet<Bundle>();
    final Set<Bundle> bundles = new TreeSet<Bundle>();
    final Map<Long, BundleInfo> bundleInfos = new HashMap<Long, BundleInfo>();
    final Map<Feature, Set<Long>> features = new HashMap<Feature, Set<Long>>();
}