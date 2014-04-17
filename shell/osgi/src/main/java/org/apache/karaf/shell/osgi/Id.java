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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.Bundle;

@Command(scope = "osgi", name = "id", description = "Gets the bundle ID.")
public class Id extends OsgiCommandSupport {

    @Argument(index = 0, name = "name", description = "The bundle name, name/version, or location", required = true, multiValued = false)
    String name;

    @Option(name = "--force", aliases = {}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean force;
    
    protected Object doExecute() throws Exception {
        return doExecute(true);
    }

    protected Object doExecute(boolean force) throws Exception {
        BundleSelector selector = new BundleSelector(bundleContext, session);
        Bundle bundle = selector.getBundle(name, force);
        
        // if name or name/version were not successful, let's try searching by location
        if (bundle == null) {
            for (int i = 0; i < bundleContext.getBundles().length; i++) {
                Bundle b = bundleContext.getBundles()[i];
                if (name.equals(b.getLocation())) {
                    bundle = b;
                    break;
                }
            }
        }
        if (bundle != null) {            
            if (force || !Util.isASystemBundle(bundleContext, bundle) || Util.accessToSystemBundleIsAllowed(bundle.getBundleId(), session)) {                
                return bundle.getBundleId();
            } else {
                System.err.println("Access to system bundle " + name + " is discouraged. You may override with -f");
            }
        } else {
            System.err.println("Bundle " + name + " is not found");
        }
        return null;
    }

}
