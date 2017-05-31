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
package org.apache.karaf.config.command.completers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.karaf.config.core.impl.MetaServiceCaller;
import org.apache.karaf.config.core.impl.MetatypeCallable;
import org.apache.karaf.shell.api.action.lifecycle.Init;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;

@Service
public class MetaCompleter implements Completer, BundleListener {
    private final StringsCompleter delegate = new StringsCompleter();
    
    @Reference
    BundleContext context;
    
    @Init
    public void init() {
        context.registerService(BundleListener.class, this, null);
        updateMeta();
    }

    @Override
    public synchronized int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        return delegate.complete(session, commandLine, candidates);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        updateMeta();
    }

    private synchronized void updateMeta() {
        List<String> pids = MetaServiceCaller.withMetaTypeService(context, new MetatypeCallable<List<String>>() {

            @Override
            public List<String> callWith(MetaTypeService metatypeService) {
                List<String> pids = new ArrayList<>();
                Bundle[] bundles = context.getBundles();
                for (Bundle bundle : bundles) {
                    
                    MetaTypeInformation info = metatypeService.getMetaTypeInformation(bundle);
                    if (info == null) {
                        continue;
                    }
                    if (info.getFactoryPids() != null) {
                        pids.addAll(Arrays.asList(info.getFactoryPids()));
                    }
                    if (info.getPids() != null) {
                        pids.addAll(Arrays.asList(info.getPids()));
                    }
                }
                return pids;
            }
        });
        if (pids != null) {
            delegate.getStrings().clear();
            delegate.getStrings().addAll(pids);
        }
    }

}
