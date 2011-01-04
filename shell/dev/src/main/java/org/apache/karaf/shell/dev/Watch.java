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
package org.apache.karaf.shell.dev;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.dev.watch.BundleWatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.ArrayList;
import java.util.List;

@Command(scope = "dev", name = "watch", description = "Watch and Update bundles")
public class Watch extends OsgiCommandSupport {

    @Argument(index = 0, name = "urls", description = "The bundle URLs", required = false)
    String urls;

    @Option(name = "-i", aliases = {}, description = "Watch interval", required = false, multiValued = false)
    private long interval;

    @Option(name = "--stop", description = "Stops watching all bundles", required = false, multiValued = false)
    protected boolean stop;

    @Option(name = "--remove", description = "Removes bundles from the watch list", required = false, multiValued = false)
    protected boolean remove;

    @Option(name = "--list", description = "Displays the watch list", required = false, multiValued = false)
    protected boolean list;


    @Override
    protected Object doExecute() throws Exception {
        //Set the interval if exists.
        if (interval > 0) {
            BundleWatcher.getInstance().setInterval(interval);
        }

        if (stop || list) {
            doExecute(null);
            return null;
        } else if (urls != null && urls.trim().length() > 0) {

            List<Bundle> bundleList = new ArrayList<Bundle>();
            //Check if an id is passed instead of URLs
            try {
                Long id = Long.parseLong(urls);
                Bundle bundle = getBundleContext().getBundle(id);
                if (bundle != null) {
                    bundleList.add(bundle);
                }
            } catch (NumberFormatException e) {

                for (int i = 0; i < getBundleContext().getBundles().length; i++) {
                    Bundle bundle = getBundleContext().getBundles()[i];
                    if (wildCardMatch(bundle.getLocation(), urls)) {
                        bundleList.add(bundle);
                    }
                }
            }
            if (bundleList.size() > 0) {
                doExecute(bundleList);
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     * Exectues watch/stop watching the passed bundles.
     *
     * @param bundleList
     */
    public void doExecute(List<Bundle> bundleList) {
        BundleWatcher watcher = BundleWatcher.getInstance();
        if (stop) {
            watcher.stop();
        } else if (list) {
            List<Bundle> watchList = watcher.getWatchList();
            if (watchList != null && watchList.size() > 0) {
                String format = "%6s %-40s";
                System.out.println(String.format(format, "ID", "Name"));
                for (Bundle bundle : watcher.getWatchList()) {
                    System.out.println(String.format(format, bundle.getBundleId(), (String) bundle.getHeaders().get(Constants.BUNDLE_NAME)));
                }
            } else System.out.println("No bundle is being watched.");
        } else {
            if (remove) {
                watcher.remove(bundleList);
            } else {
                watcher.add(bundleList);
            }
        }
    }


    /**
     * Matches text using a pattern containing wildchards.
     *
     * @param text
     * @param pattern
     * @return
     */
    public static boolean wildCardMatch(String text, String pattern) {
        String[] cards = pattern.split("\\*");
        // Iterate over the cards.
        for (String card : cards) {
            int idx = text.indexOf(card);
            // Card not detected in the text.
            if (idx == -1) {
                return false;
            }

            // Move ahead, towards the right of the text.
            text = text.substring(idx + card.length());
        }
        return true;
    }
}








