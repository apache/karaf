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
import java.util.Map;

/**
 * Periodically scan and update snapshot bundles matching a given bundle
 * symbolic name pattern.
 * <p>
 * Requires maven repository update policy set to "always" either globally or
 * for individual remote maven repositories. See pax-url-aether.
 */
public interface BundleScanner {

	/**
	 * Add bundle symbolic name match pattern.
	 * 
	 * @return true if regex is valid and was added.
	 */
	boolean add(String regex);

	/**
	 * Remove bundle symbolic name match pattern.
	 * 
	 * @return true, if regex was removed.
	 */
	boolean remove(String regex);

	/**
	 * List of matching patterns.
	 * 
	 * @return list of regex expressoins.
	 */
	List<String> getPatterns();

	/**
	 * Remove all bundle symbolic name match patterns.
	 */
	void clearPatterns();

	/**
	 * Start scanner service.
	 */
	void start();

	/**
	 * Stop scanner service.
	 */
	void stop();

	/**
	 * Set scanner invocation interval.
	 */
	void setInterval(long interval);

	/**
	 * Bundle update statistics:
	 * <p>
	 * [bundle-symbolic-name : number-of-updates]
	 */
	Map<String, Integer> getStatistics();

	/**
	 * Reset bundle update statistics:
	 */
	void clearStatistics();
	
	/**
	 * Verify scanner run status.
	 * 
	 * @return true, if scanner is running.
	 */
	boolean isRunning();

}
