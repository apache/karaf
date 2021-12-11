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
package org.apache.karaf.config.core.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Allows to use the MetaTypeService as an optional dependency
 */
public class MetaServiceCaller {

    public static <T> T withMetaTypeService(BundleContext context, Function<MetaTypeService, T> callable) {
        ServiceReference<MetaTypeService> ref = context.getServiceReference(MetaTypeService.class);
        if (ref != null) {
            try {
                MetaTypeService metaService = context.getService(ref);
                return callable.apply(metaService);
            } finally {
                context.ungetService(ref);
            }
        }
        return null;
    }

    public static List<String> getPidsWithMetaInfo(BundleContext context) {
        return withMetaTypeService(context, metatypeService -> {
            List<String> pids1 = new ArrayList<>();
            Bundle[] bundles = context.getBundles();
            if (metatypeService != null) {
                for (Bundle bundle : bundles) {
                    MetaTypeInformation info = metatypeService.getMetaTypeInformation(bundle);
                    if (info == null) {
                        continue;
                    }
                    if (info.getFactoryPids() != null) {
                        pids1.addAll(Arrays.asList(info.getFactoryPids()));
                    }
                    if (info.getPids() != null) {
                        pids1.addAll(Arrays.asList(info.getPids()));
                    }
                }
            }
            return pids1;
        });
    }
    
    /**
     * Attempts to find MetaType information for the specified PID and 
     * invokes the supplied callback with the result of the search   
     */
    public static <T> T doWithMetaType(BundleContext context, String pid, Function<Optional<MetaInfo>, T> function) {
        ServiceReference<MetaTypeService> ref = context.getServiceReference(MetaTypeService.class);
        if (ref != null) {
            try {
                MetaTypeService metaService = context.getService(ref);
                MetaInfo metaInfo = getMetatype(context, metaService, pid);
                return function.apply(Optional.ofNullable(metaInfo));
            } finally {
                context.ungetService(ref);
            }
        }
        return null;
    }
    
    private static MetaInfo getMetatype(BundleContext context, MetaTypeService metaTypeService, String pid) {
        if (metaTypeService != null) {
            for (Bundle bundle : context.getBundles()) {
                MetaTypeInformation info = metaTypeService.getMetaTypeInformation(bundle);
                if (info == null) {
                    continue;
                }
                String[] pids = info.getPids();
                for (String cPid : pids) {
                    if (cPid.equals(pid)) {
                        return new MetaInfo(info.getObjectClassDefinition(cPid, null), false);
                    }
                }
                pids = info.getFactoryPids();
                for (String cPid : pids) {
                    if (cPid.equals(pid)) {
                        return new MetaInfo(info.getObjectClassDefinition(cPid, null), true);
                    }
                }
            }
        }
        return null;
    }
    
    public static class MetaInfo {
        private final ObjectClassDefinition definition;
        private final boolean factory;
        
        MetaInfo(ObjectClassDefinition definition, boolean factory) {

            this.definition = definition;
            this.factory = factory;
        }
        
        public ObjectClassDefinition getDefinition() {

            return definition;
        }
        
        public boolean isFactory() {

            return factory;
        }
        
    }
}
