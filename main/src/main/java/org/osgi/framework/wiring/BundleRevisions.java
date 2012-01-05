/*
 * Copyright (c) OSGi Alliance (2011). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.framework.wiring;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * The {@link BundleRevision bundle revisions} of a bundle. When a bundle is
 * installed and each time a bundle is updated, a new bundle revision of the
 * bundle is created. For a bundle that has not been uninstalled, the most
 * recent bundle revision is defined to be the current bundle revision. A bundle
 * in the UNINSTALLED state does not have a current revision. An in use bundle
 * revision is associated with an {@link BundleWiring#isInUse() in use}
 * {@link BundleWiring}. The current bundle revision, if there is one, and all
 * in use bundle revisions are returned.
 * 
 * <p>
 * The bundle revisions for a bundle can be obtained by calling
 * {@link Bundle#adapt(Class) bundle.adapt}({@link BundleRevisions}.class).
 * {@link #getRevisions()} on the bundle.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: 1d95ad10f0f08b100f8ee2485bdd34120032c7af $
 */
public interface BundleRevisions extends BundleReference {
	/**
	 * Return the bundle revisions for the {@link BundleReference#getBundle()
	 * referenced} bundle.
	 * 
	 * <p>
	 * The result is a list containing the current bundle revision, if there is
	 * one, and all in use bundle revisions. The list may also contain
	 * intermediate bundle revisions which are not in use.
	 * 
	 * <p>
	 * The list is ordered in reverse chronological order such that the first
	 * item is the most recent bundle revision and last item is the oldest
	 * bundle revision.
	 * 
	 * <p>
	 * Generally the list will have at least one bundle revision for the bundle:
	 * the current bundle revision. However, for an uninstalled bundle with no
	 * in use bundle revisions, the list may be empty.
	 * 
	 * @return A list containing a snapshot of the {@link BundleRevision}s for
	 *         the referenced bundle.
	 */
	List<BundleRevision> getRevisions();
}
