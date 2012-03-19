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

import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.MultiException;
import org.osgi.framework.Bundle;

/**
 * Command related to bundles requiring read-write access to the bundle (including system bundle).
 */
public abstract class BundlesCommandWithConfirmation extends BundlesCommand {

    @Option(name = "--force", aliases = {"-f"}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean force;
    
    protected String errorMessage = "Unable to execute command on bundle ";
    
    public BundlesCommandWithConfirmation() {
        super(false);
    }

    protected Object doExecute() throws Exception {
        doExecute(force);
        return null;
    }

    protected void doExecute(List<Bundle> bundles) throws Exception {
        if (bundles.isEmpty()) {
            System.err.println("No bundles specified.");
            return;
        }
        List<Exception> exceptions = new ArrayList<Exception>();
        for (Bundle bundle : bundles) {
            try {
                executeOnBundle(bundle);
            } catch (Exception e) {
                exceptions.add(new Exception(errorMessage + bundle.getBundleId() + ": " + e.getMessage(), e));
            }
        }
        MultiException.throwIf("Error executing command on bundles", exceptions);
    }
    
    protected abstract void executeOnBundle(Bundle bundle) throws Exception;
}
