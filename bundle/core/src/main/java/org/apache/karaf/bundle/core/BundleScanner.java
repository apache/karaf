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

/**
 * Periodically scan and update snapshot bundles matching a given bundle
 * symbolic name pattern.
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
	boolean remove(String pattern);

	/**
	 * Start scanner service.
	 * 
	 * @return true, if service was started.
	 */
	boolean start();

	/**
	 * Stop scanner service.
	 * 
	 * @return true, if service was stopped.
	 */
	boolean stop();

	/**
	 * Set scanner invocation interval.
	 */
	void setInterval(long interval);

}
