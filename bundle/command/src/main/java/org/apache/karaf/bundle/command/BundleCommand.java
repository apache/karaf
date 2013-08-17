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

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.util.ShellUtil;
import org.osgi.framework.Bundle;

/**
 * Unique bundle command.
 */
public abstract class BundleCommand extends OsgiCommandSupport {

    @Argument(index = 0, name = "id", description = "The bundle ID or name or name/version", required = true, multiValued = false)
    String id;

    boolean defaultAllBundles = true;

    BundleService bundleService;

    public BundleCommand(boolean defaultAllBundles) {
        this.defaultAllBundles = defaultAllBundles;
    }

    protected Object doExecute() throws Exception {
        return doExecute(true);
    }

    protected Object doExecute(boolean force) throws Exception {
        Bundle bundle = bundleService.getBundle(id, defaultAllBundles);
        if (bundle != null) {
            if (force || !ShellUtil.isASystemBundle(bundleContext, bundle)) {
                doExecute(bundle);
            } else {
                System.err.println("Access to system bundle " + id + " is discouraged. You may override with -f");
            }
        }
        return null;
    }

    protected abstract void doExecute(Bundle bundle) throws Exception;

    public void setBundleService(BundleService bundleService) {
        this.bundleService = bundleService;
    }

}
