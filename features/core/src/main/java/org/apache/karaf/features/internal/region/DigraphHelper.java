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
package org.apache.karaf.features.internal.region;

import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.apache.karaf.util.json.JsonReader;
import org.apache.karaf.util.json.JsonWriter;
import org.eclipse.equinox.internal.region.StandardRegionDigraph;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

public final class DigraphHelper {

    public static final String DIGRAPH_FILE = "digraph.json";

    private static final String REGIONS = "regions";
    private static final String EDGES = "edges";
    private static final String TAIL = "tail";
    private static final String HEAD = "head";
    private static final String POLICY = "policy";

    private DigraphHelper() {
    }

    public static StandardRegionDigraph loadDigraph(BundleContext bundleContext) throws BundleException, IOException, InvalidSyntaxException {
        StandardRegionDigraph digraph;
        ThreadLocal<Region> threadLocal = new ThreadLocal<>();
        File digraphFile = bundleContext.getDataFile(DIGRAPH_FILE);
        if (digraphFile == null || !digraphFile.exists()) {
            digraph = new StandardRegionDigraph(bundleContext, threadLocal);
        } else {
            try (
                    InputStream in = new FileInputStream(digraphFile)
            ) {
                digraph = readDigraph(new DataInputStream(in), bundleContext, threadLocal);
            }
        }
        return digraph;
    }

    public static void saveDigraph(File outFile, RegionDigraph digraph) {
        try (
            FileOutputStream out = new FileOutputStream(outFile)
        ) {
            saveDigraph(digraph, out);
        } catch (Exception e) {
            // Ignore
        }
    }

    @SuppressWarnings({
     "unchecked", "rawtypes"
    })
    static StandardRegionDigraph readDigraph(InputStream in, BundleContext bundleContext, ThreadLocal<Region> threadLocal) throws IOException, BundleException, InvalidSyntaxException {
        StandardRegionDigraph digraph = new StandardRegionDigraph(bundleContext, threadLocal);
        Map json = (Map) JsonReader.read(in);
        Map<String, Collection<Number>> regions = (Map<String, Collection<Number>>) json.get(REGIONS);
        for (Map.Entry<String, Collection<Number>> rmap : regions.entrySet()) {
            String name = rmap.getKey();
            Region region = digraph.createRegion(name);
            for (Number b : rmap.getValue()) {
                region.addBundle(b.longValue());
            }
        }
        List<Map<String, Object>> edges = (List<Map<String, Object>>) json.get(EDGES);
        for (Map<String, Object> e : edges) {
            String tail = (String) e.get(TAIL);
            String head = (String) e.get(HEAD);
            Map<String, Collection<String>> policy = (Map<String, Collection<String>>) e.get(POLICY);
            RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
            for (Map.Entry<String, Collection<String>> rf : policy.entrySet()) {
                String ns = rf.getKey();
                for (String f : rf.getValue()) {
                    builder.allow(ns, f);
                }
            }
            digraph.connect(digraph.getRegion(tail), builder.build(), digraph.getRegion(head));
        }
        return digraph;
    }

    static void saveDigraph(RegionDigraph digraph, OutputStream out) throws IOException {
        Map<String, Object> json = new LinkedHashMap<>();
        Map<String, Set<Long>> regions = new LinkedHashMap<>();
        json.put(REGIONS, regions);
        for (Region region : digraph.getRegions()) {
            regions.put(region.getName(), region.getBundleIds());
        }
        List<Map<String, Object>> edges = new ArrayList<>();
        json.put(EDGES, edges);
        for (Region tail : digraph.getRegions()) {
            for (RegionDigraph.FilteredRegion fr : digraph.getEdges(tail)) {
                Map<String, Object> edge = new HashMap<>();
                edge.put(TAIL, tail.getName());
                edge.put(HEAD, fr.getRegion().getName());
                edge.put(POLICY, fr.getFilter().getSharingPolicy());
                edges.add(edge);

            }
        }
        JsonWriter.write(out, json);
    }

    public static Map<String, Set<Long>> getBundlesPerRegion(RegionDigraph digraph) {
        Map<String, Set<Long>> bundlesPerRegion = new HashMap<>();
        if (digraph != null) {
            for (Region region : digraph.getRegions()) {
                bundlesPerRegion.put(region.getName(), new HashSet<>(region.getBundleIds()));
            }
        }
        return bundlesPerRegion;
    }
    
    public static Map<String, Map<String, Map<String, Set<String>>>> getPolicies(RegionDigraph digraph) {
        Map<String, Map<String, Map<String, Set<String>>>> filtersPerRegion = new HashMap<>();
        if (digraph == null) {
            return filtersPerRegion;
        }
        for (Region region : digraph.getRegions()) {
            Map<String, Map<String, Set<String>>> edges = new HashMap<>();
            for (RegionDigraph.FilteredRegion fr : digraph.getEdges(region)) {
                Map<String, Set<String>> policy = new HashMap<>();
                Map<String, Collection<String>> current = fr.getFilter().getSharingPolicy();
                for (Map.Entry<String, Collection<String>> entry : current.entrySet()) {
                	for(String f : entry.getValue()) {
                		 addToMapSet(policy, entry.getKey(), f);
                	}
                }
                edges.put(fr.getRegion().getName(), policy);
            }
            filtersPerRegion.put(region.getName(), edges);
        }
        return filtersPerRegion;
    }

    public static void verifyUnmanagedBundles(BundleContext bundleContext, RegionDigraph dg) throws BundleException {
        // Create default region is missing
        Region defaultRegion = dg.getRegion(FeaturesServiceImpl.ROOT_REGION);
        if (defaultRegion == null) {
            defaultRegion = dg.createRegion(FeaturesServiceImpl.ROOT_REGION);
        }
        dg.setDefaultRegion(defaultRegion);
        // Add all unknown bundle to default region
        Set<Long> ids = new HashSet<>();
        for (Bundle bundle : bundleContext.getBundles()) {
            long id = bundle.getBundleId();
            ids.add(id);
            if (dg.getRegion(id) == null) {
                defaultRegion.addBundle(id);
            }
        }
        // Clean stalled bundles
        for (Region region : dg) {
            Set<Long> bundleIds = new HashSet<>(region.getBundleIds());
            bundleIds.removeAll(ids);
            for (long id : bundleIds) {
                region.removeBundle(id);
            }
        }
    }
}
