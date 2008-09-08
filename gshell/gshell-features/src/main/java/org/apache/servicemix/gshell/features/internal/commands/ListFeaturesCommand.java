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
package org.apache.servicemix.gshell.features.internal.commands;

import org.apache.servicemix.gshell.features.FeaturesService;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.clp.Option;

@CommandComponent(id="features:list", description="List existing features.")
public class ListFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-i", aliases={"--installed"}, description="Display the list of installed features")
    boolean installed;

    protected void doExecute(FeaturesService admin) throws Exception {
        String[] features;
        if (installed) {
            features = admin.listInstalledFeatures();
        } else {
        	// Print column headers.
        	io.out.println("  State          Version       Name");
            features = admin.listFeatures();
        }
        if ((features != null) && (features.length > 0)) {
            for (int i = 0; i < features.length; i++) {
                io.out.println(features[i]);
            }
        } else {
            if (installed) {
                io.out.println("No features installed.");
            } else {
                io.out.println("No features available.");
            }
        }
    }
}