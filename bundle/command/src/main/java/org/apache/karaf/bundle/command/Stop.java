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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.osgi.framework.Bundle;

@Command(scope = "bundle", name = "stop", description = "Stop bundles.")
public class Stop extends BundlesCommandWithConfirmation {
    
	@Option(name = "-t", aliases={"--transient"}, description="Keep the bundle as auto-start", required = false, multiValued = false)
	boolean transientStop;
	
    public Stop() {
        this.errorMessage = "Unable to stop bundle";
    }

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
    	if (transientStop) {
    		bundle.stop(Bundle.STOP_TRANSIENT);
    	} else {
    		bundle.stop();
    	}
    }

}
