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
package org.apache.karaf.features.command;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.console.MultiException;

@Command(scope = "features", name = "refreshUrl", description = "Reloads the list of available features from the repositories.")
public class RefreshUrlCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "urls", description = "Repository URLs to reload (leave empty for all)", required = false, multiValued = true)
    List<String> urls;

    protected void doExecute(FeaturesService admin) throws Exception {
        if (urls == null || urls.isEmpty()) {
            urls = new ArrayList<String>();
            for (Repository repo : admin.listRepositories()) {
                urls.add(repo.getURI().toString());
            }
        }
        List<Exception> exceptions = new ArrayList<Exception>();
        LinkedList<URI> uriToReload = new LinkedList<URI>();
        for (String url : urls) {
            try {
                Pattern pattern = Pattern.compile(url);
                for (Repository repository : admin.listRepositories()) {
                    URI uri = repository.getURI();
                    Matcher matcher = pattern.matcher(uri.toString());
                    if (matcher.matches()) {
                        uriToReload.add(uri);
                    }
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        for (URI uri : uriToReload) {
            try {
                System.out.println("Refreshing " + uri.toString());
                admin.removeRepository(uri);
                admin.addRepository(uri);
            } catch (Exception e) {
                exceptions.add(e);
                //get chance to restore previous, fix for KARAF-4
                admin.restoreRepository(uri);
            }
        }
        MultiException.throwIf("Unable to add repositories", exceptions);
    }
}
