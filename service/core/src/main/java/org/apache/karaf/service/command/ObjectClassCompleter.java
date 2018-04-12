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

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.framework.BundleContext;

@Service
public class ObjectClassCompleter implements Completer {

    @Reference
    private BundleContext context;

    public void setContext(BundleContext context) {
        this.context = context;
    }

    @Override
    public int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        Map<String, Integer> serviceNamesMap = ListServices.getServiceNamesMap(context);
        Set<String> serviceNames = serviceNamesMap.keySet();
        List<String> strings = new ArrayList<>();
        for (String name : serviceNames) {
            strings.add(ObjectClassMatcher.getShortName(name));
        }
        strings.addAll(serviceNames);
        return new StringsCompleter(strings).complete(session, commandLine, candidates);
    }

}
