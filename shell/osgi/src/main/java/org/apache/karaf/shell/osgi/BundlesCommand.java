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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public abstract class BundlesCommand extends OsgiCommandSupport {

    @Argument(index = 0, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = true, multiValued = true)
    List<String> ids;

    @Option(name = "--force", aliases = {}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean force;

    protected Object doExecute() throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        if (ids != null && !ids.isEmpty()) {
            for (String id : ids) {

                // id is a number
                Pattern pattern = Pattern.compile("^\\d+$");
                Matcher matcher = pattern.matcher(id);
                if (matcher.find()) {
                    Bundle bundle = this.getBundleById(id);
                    this.addBundle(bundle, id, force, bundles);
                    continue;
                }

                // id is a number range
                pattern = Pattern.compile("^(\\d+)-(\\d+)$");
                matcher = pattern.matcher(id);
                if (matcher.find()) {
                    int index = id.indexOf('-');
                    Long startId = Long.valueOf(id.substring(0, index));
                    Long endId = Long.valueOf(id.substring(index + 1));
                    if (startId < endId) {
                        for (long i = startId; i <= endId; i++) {
                            Bundle bundle = getBundleContext().getBundle(i);
                            this.addBundle(bundle, id, force, bundles);
                        }
                    }
                    continue;
                }

                Bundle bundle = null;
                int index = id.indexOf('/');
                List<Bundle> bundlesByName = null;
                if (index != -1) {
                    // user has provided name and version
                    bundlesByName = this.getBundleByNameAndVersion(id.substring(0, index), id.substring(index + 1));
                } else {
                    // user has provided only the name
                    bundlesByName = this.getBundleByName(id);
                }
                for (Bundle bundleByName : bundlesByName) {
                    this.addBundle(bundleByName, id, force, bundles);
                }

            }
        }
        doExecute(bundles);
        return null;
    }

    private void addBundle(Bundle bundle, String id, boolean force, List bundles) throws Exception {
        if (bundle == null) {
            // if the bundle is null here, it's because we didn't find it
            System.err.println("Bundle " + id + " is invalid");
        } else {
            if (force || !Util.isASystemBundle(getBundleContext(), bundle) || Util.accessToSystemBundleIsAllowed(bundle.getBundleId(), session)) {
                bundles.add(bundle);
            }
        }
    }

    /**
     * Get a bundle identified by an id number.
     *
     * @param id the id number.
     * @return the bundle or null if not found.
     */
    private Bundle getBundleById(String id) {
        Bundle bundle = null;
        try {
            long idNumber = Long.valueOf(id);
            bundle = getBundleContext().getBundle(idNumber);
        } catch (NumberFormatException nfe) {
            // ignore
        }
        return bundle;
    }

    /**
     * Get a bundles list with the name or symbolic name matching the pattern.
     *
     * @param name the bundle name or symbolic name pattern to match.
     * @return the bundles list.
     */
    private List<Bundle> getBundleByName(String name) {
        return this.getBundleByNameAndVersion(name, null);
    }

    /**
     * Get a bundles list with the name or symbolic name matching the name pattern and version matching the version pattern.
     *
     * @param name    the bundle name or symbolic name regex to match.
     * @param version the bundle version regex to match.
     * @return the bundles list.
     */
    private List<Bundle> getBundleByNameAndVersion(String name, String version) {
        Bundle[] bundles = getBundleContext().getBundles();

        ArrayList<Bundle> result = new ArrayList<Bundle>();

        Pattern namePattern = Pattern.compile(name);

        for (int i = 0; i < bundles.length; i++) {

            String bundleName = (String) bundles[i].getHeaders().get(Constants.BUNDLE_NAME);
            String bundleSymbolicName = bundles[i].getSymbolicName();

            Matcher nameMatcher = namePattern.matcher(bundleName);
            Matcher symbolicNameMatcher = namePattern.matcher(bundleSymbolicName);

            if (version != null) {

                Pattern versionPattern = Pattern.compile(version);

                String bundleVersion = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
                Matcher versionMatcher = versionPattern.matcher(bundleVersion);

                if ((nameMatcher.find() || symbolicNameMatcher.find()) && versionMatcher.find()) {
                    result.add(bundles[i]);
                }
            } else {
                if (nameMatcher.find() || symbolicNameMatcher.find()) {
                    result.add(bundles[i]);
                }
            }
        }
        return result;
    }

    protected abstract void doExecute(List<Bundle> bundles) throws Exception;
}
