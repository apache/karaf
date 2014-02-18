/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;
import org.osgi.framework.BundleContext;

@Service
public class ObjectClassCompleter implements Completer {

    @Reference
    private BundleContext context;

    public void setContext(BundleContext context) {
        this.context = context;
    }

    @SuppressWarnings("rawtypes")
    public int complete(final String buffer, final int cursor, final List candidates) {
        Map<String, Integer> serviceNamesMap = ListServices.getServiceNamesMap(context);
        Set<String> serviceNames = serviceNamesMap.keySet();
        List<String> strings = new ArrayList<String>();
        for (String name : serviceNames) {
            strings.add(ObjectClassMatcher.getShortName(name));
        }
        strings.addAll(serviceNames);
        return new StringsCompleter(strings).complete(buffer, cursor, candidates);
    }

}
