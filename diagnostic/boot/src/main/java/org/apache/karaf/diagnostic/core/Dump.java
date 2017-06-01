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
package org.apache.karaf.diagnostic.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.diagnostic.core.common.DirectoryDumpDestination;
import org.apache.karaf.diagnostic.core.common.ZipDumpDestination;
import org.apache.karaf.diagnostic.core.providers.BundleDumpProvider;
import org.apache.karaf.diagnostic.core.providers.EnvironmentDumpProvider;
import org.apache.karaf.diagnostic.core.providers.HeapDumpProvider;
import org.apache.karaf.diagnostic.core.providers.MemoryDumpProvider;
import org.apache.karaf.diagnostic.core.providers.ThreadDumpProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Dump helper
 */
public final class Dump {

    public static DumpDestination directory(File file) {
        return new DirectoryDumpDestination(file);
    }

    public static DumpDestination zip(File file) {
        return new ZipDumpDestination(file);
    }

    public static void dump(BundleContext bundleContext, DumpDestination destination, boolean noThreadDump, boolean noHeapDump) {
        List<DumpProvider> providers = new ArrayList<>();
        providers.add(new EnvironmentDumpProvider(bundleContext));
        providers.add(new MemoryDumpProvider());
        if (!noThreadDump) providers.add(new ThreadDumpProvider());
        if (!noHeapDump) providers.add(new HeapDumpProvider());
        providers.add(new BundleDumpProvider(bundleContext));
        for (DumpProvider provider : providers) {
            try {
                provider.createDump(destination);
            } catch (Throwable t) {
                // Ignore
            }
        }
        try {
            for (ServiceReference<DumpProvider> ref : bundleContext.getServiceReferences(DumpProvider.class, null)) {
                DumpProvider provider = bundleContext.getService(ref);
                try {
                    provider.createDump(destination);
                } catch (Throwable t) {
                    // Ignore
                } finally {
                    bundleContext.ungetService(ref);
                }
            }
        } catch (InvalidSyntaxException e) {
            // Ignore
        }
        try {
            destination.save();
        } catch (Throwable t) {
            // Ignore
        }
    }

    // Private constructor
    private Dump() { }
}
