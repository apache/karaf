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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.osgi.framework.BundleContext;

public class ObjectClassCompleter implements Completer {

    private final StringsCompleter delegate = new StringsCompleter();

    private BundleContext context;

    public void setContext(BundleContext context) {
        this.context = context;
    }

    @SuppressWarnings("rawtypes")
    public int complete(final String buffer, final int cursor, final List candidates) {
        delegate.getStrings().clear();
        Map<String, Integer> serviceNamesMap = ListServices.getServiceNamesMap(context);
        Set<String> serviceNames = serviceNamesMap.keySet();
        for (String name : serviceNames) {
            delegate.getStrings().add(ObjectClassMatcher.getShortName(name));
        }
        delegate.getStrings().addAll(serviceNames);
        return delegate.complete(buffer, cursor, candidates);
    }



}
