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

import org.apache.karaf.bundle.command.completers.BundleSymbolicNameCompleter;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Unique bundle command.
 */
public abstract class BundleCommand implements Action {

    @Option(name = "--context", description = "Use the given bundle context")
    String context = "0";

    @Argument(index = 0, name = "id", description = "The bundle ID or name or name/version", required = true, multiValued = false)
    @Completion(BundleSymbolicNameCompleter.class)
    String id;

    @Reference
    BundleService bundleService;

    @Reference
    BundleContext bundleContext;

    public Object execute() throws Exception {
        Bundle bundle = bundleService.getBundle(id);
        return doExecute(bundle);
    }

    protected abstract Object doExecute(Bundle bundle) throws Exception;

    public void setBundleService(BundleService bundleService) {
        this.bundleService = bundleService;
    }

}
