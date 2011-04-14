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
package org.apache.karaf.shell.osgi;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.Bundle;

public abstract class BundlesCommandOptional extends OsgiCommandSupport {
    
    @Argument(index = 0, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = false, multiValued = true)
    List<String> ids;

    @Option(name = "--force", aliases = {}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean force;

    protected Object doExecute() throws Exception {
        List<Bundle> bundles = null;
        if (ids != null && !ids.isEmpty()) {
            BundleSelector selector = new BundleSelector(getBundleContext(), session);      
            bundles = selector.selectBundles(ids, force);
        }
        doExecute(bundles);
        return null;
    }

    /**
     * 
     * @param bundles null if no bundle ids or names were specified.
     * @throws Exception
     */
    protected abstract void doExecute(List<Bundle> bundles) throws Exception;
}
