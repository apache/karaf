/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.bundle.core;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

public interface BundleWatcher {

    void bundleChanged(BundleEvent event);

    /**
     * Add a Bundle URLs to the watch list.
     * 
     * @param url the URL to watch.
     */
    void add(String url);

    /**
     * Remove a bundle URLs from the watch list.
     * 
     * @param url the URL to stop to watch.
     */
    void remove(String url);

    /**
     * Return the bundles that match
     * 
     * @param url the bundles URL.
     * @return the list of bundles.
     */
    List<Bundle> getBundlesByURL(String url);

    List<String> getWatchURLs();

    void setWatchURLs(List<String> watchURLs);

    void start();

    void stop();

    void setInterval(long interval);

}
