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
package org.apache.karaf.bundle.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.bundle.command.completers.BundleSymbolicNameCompleter;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.support.MultiException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public abstract class BundlesCommand implements Action {

    @Option(name = "--context", aliases = {"-c"}, description = "Use the given bundle context")
    String context = "0";

    @Argument(index = 0, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = false, multiValued = true)
    @Completion(BundleSymbolicNameCompleter.class)
    List<String> ids;
    
    @Reference
    BundleContext bundleContext;

    @Reference
    BundleService bundleService;

    protected boolean defaultAllBundles = true;

    protected String errorMessage = "Unable to execute command on bundle ";

    @Override
    public Object execute() throws Exception {
        List<Bundle> bundles =  bundleService.selectBundles(context, ids, defaultAllBundles);
        return doExecute(bundles);
    }
    
    protected Object doExecute(List<Bundle> bundles) throws Exception {
        if (bundles.isEmpty()) {
            throw new IllegalArgumentException("No matching bundles");
        }
        List<Exception> exceptions = new ArrayList<>();
        for (Bundle bundle : bundles) {
            try {
                executeOnBundle(bundle);
            } catch (Exception e) {
                exceptions.add(new Exception(errorMessage + bundle.getBundleId() + ": " + e.getMessage(), e));
            }
        }
        MultiException.throwIf("Error executing command on bundles", exceptions);
        return null;
    }

    protected abstract void executeOnBundle(Bundle bundle) throws Exception;

    public void setBundleService(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
