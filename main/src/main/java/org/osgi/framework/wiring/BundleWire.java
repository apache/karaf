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

/**
 * A wire connecting a {@link BundleCapability} to a {@link BundleRequirement}.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: 4f936a84065762ec3267a44f86ae01b0150e44ce $
 */
public interface BundleWire {
	/**
	 * Returns the {@link BundleCapability} for this wire.
	 * 
	 * @return The {@link BundleCapability} for this wire.
	 */
	BundleCapability getCapability();

	/**
	 * Return the {@link BundleRequirement} for this wire.
	 * 
	 * @return The {@link BundleRequirement} for this wire.
	 */
	BundleRequirement getRequirement();

	/**
	 * Returns the bundle wiring {@link BundleWiring#getProvidedWires(String)
	 * providing} the {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The bundle revision referenced by the returned bundle wiring may differ
	 * from the bundle revision reference by the {@link #getCapability()
	 * capability}.
	 * 
	 * @return The bundle wiring providing the capability. If the bundle wiring
	 *         providing the capability is not {@link BundleWiring#isInUse() in
	 *         use}, {@code null} will be returned.
	 */
	BundleWiring getProviderWiring();

	/**
	 * Returns the bundle wiring who
	 * {@link BundleWiring#getRequiredWires(String) requires} the
	 * {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The bundle revision referenced by the returned bundle wiring may differ
	 * from the bundle revision reference by the {@link #getRequirement()
	 * requirement}.
	 * 
	 * @return The bundle wiring whose requirement is wired to the capability.
	 *         If the bundle wiring requiring the capability is not
	 *         {@link BundleWiring#isInUse() in use}, {@code null} will be
	 *         returned.
	 */
	BundleWiring getRequirerWiring();
}
