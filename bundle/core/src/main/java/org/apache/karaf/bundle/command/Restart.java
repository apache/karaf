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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.MultiException;
import org.osgi.framework.Bundle;

@Command(scope = "bundle", name = "restart", description = "Restarts bundles.")
@Service
public class Restart extends BundlesCommand {
    
    public Restart() {
        defaultAllBundles = false;
        errorMessage = "Error restarting bundle";
    }

    protected Object doExecute(List<Bundle> bundles) throws Exception {
        if (bundles.isEmpty()) {
            System.err.println("No bundles specified.");
            return null;
        }
        List<Exception> exceptions = new ArrayList<>();
        for (Bundle bundle : bundles) {
            try {
                bundle.stop(Bundle.STOP_TRANSIENT);
            } catch (Exception e) {
                exceptions.add(new Exception("Unable to stop bundle " + bundle.getBundleId() + ": " + e.getMessage(), e));
            }
        }
        for (Bundle bundle : bundles) {
            try {
                bundle.start(Bundle.START_TRANSIENT);
            } catch (Exception e) {
                exceptions.add(new Exception("Unable to start bundle " + bundle.getBundleId() + ": " + e.getMessage(), e));
            }
        }
        MultiException.throwIf("Error restarting bundles", exceptions);
        return null;
    }

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
    }

}
