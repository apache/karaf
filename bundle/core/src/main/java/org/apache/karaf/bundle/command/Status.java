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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;

@Command(scope = "bundle", name = "status", description = "Get the bundle current status")
@Service
public class Status extends BundleCommand {

    @Override
    protected Object doExecute(Bundle bundle) throws Exception {
        System.out.println(getState(bundle));
        return null;
    }

    /**
     * Return a String representation of a bundle state
     */
    private String getState(Bundle bundle) {
        switch (bundle.getState()) {
            case Bundle.UNINSTALLED:
                return "Uninstalled";
            case Bundle.INSTALLED:
                return "Installed";
            case Bundle.RESOLVED:
                return "Resolved";
            case Bundle.STARTING:
                return "Starting";
            case Bundle.STOPPING:
                return "Stopping";
            case Bundle.ACTIVE:
                return "Active";
            default:
                return "Unknown";
        }
    }

}
