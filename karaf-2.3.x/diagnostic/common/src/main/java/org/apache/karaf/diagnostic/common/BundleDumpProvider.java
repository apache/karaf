/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.common;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.diagnostic.core.common.TextDumpProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Dump provider which produces file named bundles.txt with list of
 * installed bundles and it's state.
 */
public class BundleDumpProvider extends TextDumpProvider {

    /**
     * Static map with state mask to string representation.
     */
    private static Map<Integer, String> stateMap = new HashMap<Integer, String>();

    /**
     * Map bundle states to string representation.
     */
    static {
        stateMap.put(0x00000001, "UNINSTALLED");
        stateMap.put(0x00000002, "INSTALLED");
        stateMap.put(0x00000004, "RESOLVED");
        stateMap.put(0x00000008, "STARTING");
        stateMap.put(0x00000010, "STOPPING");
        stateMap.put(0x00000020, "ACTIVE");
    }

    /**
     * Bundle context.
     */
    private BundleContext bundleContext;

    /**
     * Creates new bundle information file.
     *  
     * @param context Bundle context to access framework state.
     */
    public BundleDumpProvider(BundleContext context) {
        super("bundles.txt");
        this.bundleContext = context;
    }

    /**
     * {@inheritDoc}
     */
    protected void writeDump(OutputStreamWriter writer) throws IOException {
        // get bundle states
        Bundle[] bundles = bundleContext.getBundles();

        writer.write("Number of installed bundles " + bundles.length + "\n");

        // create file header
        writer.write("Id\tSymbolic name\tState\n");
        for (Bundle bundle : bundles) {
            // write row :)
            writer.write(bundle.getBundleId() + "\t" + bundle.getSymbolicName()
                + "\t" + stateMap.get(bundle.getState()) + "\n");
        }

        writer.flush();
    }

}
