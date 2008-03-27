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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.servicemix.gshell.features.FeaturesService;
import org.apache.servicemix.gshell.features.Repository;

@CommandComponent(id="features:refreshUrl", description="Reload the repositories to obtain a fresh list of features")
public class RefreshUrlCommand extends FeaturesCommandSupport {

    @Argument(required = false, multiValued = true, description = "Repository URLs (leave empty for all)")
    List<String> urls;

    protected void doExecute(FeaturesService admin) throws Exception {
        if (urls == null || urls.isEmpty()) {
            urls = new ArrayList<String>();
            for (Repository repo : admin.listRepositories()) {
                urls.add(repo.getURL().toString());
            }
        }
        for (String strUrl : urls) {
            URL url = new URL(strUrl);
            admin.removeRepository(url);
            admin.addRepository(url);
        }
    }
}