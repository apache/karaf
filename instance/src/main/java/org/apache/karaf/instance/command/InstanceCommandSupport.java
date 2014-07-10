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
package org.apache.karaf.instance.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

public abstract class InstanceCommandSupport implements Action {

    @Reference
    private InstanceService instanceService;

    public InstanceService getInstanceService() {
        return instanceService;
    }

    public void setInstanceService(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    protected Instance getExistingInstance(String name) {
        Instance i = instanceService.getInstance(name);
        if (i == null) {
            throw new IllegalArgumentException("Instances '" + name + "' does not exist");
        }
        return i;
    }

    protected List<Instance> getMatchingInstances(List<String> patterns) {
        List<Instance> instances = new ArrayList<>();
        Instance[] allInstances = instanceService.getInstances();
        for (Instance instance : allInstances) {
            if (match(instance.getName(), patterns)) {
                instances.add(instance);
            }
        }
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("No matching instances");
        }
        return instances;
    }

    protected static Map<String, URL> getResources(List<String> resources) throws MalformedURLException {
        Map<String, URL> result = new HashMap<>();
        if (resources != null) {
            for (String resource : resources) {
                String path = resource.substring(0, resource.indexOf("="));
                String location = resource.substring(path.length() + 1);
                URL url = new URL(location);
                result.put(path, url);
            }
        }
        return result;
    }

    private boolean match(String name, List<String> patterns) {
        for (String pattern : patterns) {
            if (name.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object execute() throws Exception {
        return doExecute();
    }

    protected abstract Object doExecute() throws Exception;
}
