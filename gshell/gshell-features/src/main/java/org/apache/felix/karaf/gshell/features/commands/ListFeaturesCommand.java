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
package org.apache.felix.karaf.gshell.features.commands;

import org.apache.felix.karaf.gshell.features.FeaturesService;
import org.apache.felix.gogo.commands.Option;

public class ListFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-i", aliases={"--installed"}, description="Display the list of installed features")
    boolean installed;

    protected void doExecute(FeaturesService admin) throws Exception {
        String[] features;
        if (installed) {
            features = admin.listInstalledFeatures();
        } else {
        	// Print column headers.
        	System.out.println("  State          Version       Name");
            features = admin.listFeatures();
        }
        if ((features != null) && (features.length > 0)) {
            for (int i = 0; i < features.length; i++) {
                System.out.println(features[i]);
            }
        } else {
            if (installed) {
                System.out.println("No features installed.");
            } else {
                System.out.println("No features available.");
            }
        }
    }
}
