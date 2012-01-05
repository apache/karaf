/*
 * Copyright (c) OSGi Alliance (2001, 2010). All Rights Reserved.
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

import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkListener;

/**
 * Query and modify wiring information for the framework. The framework wiring
 * object for the framework can be obtained by calling
 * {@link Bundle#adapt(Class) bundle.adapt(FrameworkWiring.class)} on the system
 * bundle. Only the system bundle can be adapted to a FrameworkWiring object.
 * 
 * <p>
 * The system bundle associated with this FrameworkWiring object can be obtained
 * by calling {@link BundleReference#getBundle()}.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: f9f3f89b5b8d369453d645a52a482a385a2bd520 $
 */
public interface FrameworkWiring extends BundleReference {
	/**
	 * Refreshes the specified bundles. This forces the update (replacement) or
	 * removal of packages exported by the specified bundles.
	 * 
	 * <p>
	 * The technique by which the framework refreshes bundles may vary among
	 * different framework implementations. A permissible implementation is to
	 * stop and restart the framework.
	 * 
	 * <p>
	 * This method returns to the caller immediately and then performs the
	 * following steps on a separate thread:
	 * 
	 * <ol>
	 * <li>Compute the {@link #getDependencyClosure(Collection) dependency
	 * closure} of the specified bundles. If no bundles are specified, compute
	 * the dependency closure of the {@link #getRemovalPendingBundles() removal
	 * pending} bundles.
	 * 
	 * <li>Each bundle in the dependency closure that is in the {@code ACTIVE}
	 * state will be stopped as described in the {@code Bundle.stop} method.
	 * 
	 * <li>Each bundle in the dependency closure that is in the {@code RESOLVED}
	 * state is unresolved and thus moved to the {@code INSTALLED} state. The
	 * effect of this step is that bundles in the dependency closure are no
	 * longer {@code RESOLVED}.
	 * 
	 * <li>Each bundle in the dependency closure that is in the
	 * {@code UNINSTALLED} state is removed from the dependency closure and is
	 * now completely removed from the Framework.
	 * 
	 * <li>Each bundle in the dependency closure that was in the {@code ACTIVE}
	 * state prior to Step 2 is started as described in the {@code Bundle.start}
	 * method, causing all bundles required for the restart to be resolved. It
	 * is possible that, as a result of the previous steps, packages that were
	 * previously exported no longer are. Therefore, some bundles may be
	 * unresolvable until bundles satisfying the dependencies have been
	 * installed in the Framework.
	 * </ol>
	 * 
	 * <p>
	 * For any exceptions that are thrown during any of these steps, a framework
	 * event of type {@code FrameworkEvent.ERROR} is fired containing the
	 * exception. The source bundle for these events should be the specific
	 * bundle to which the exception is related. If no specific bundle can be
	 * associated with the exception then the System Bundle must be used as the
	 * source bundle for the event. All framework events fired by this method
	 * are also delivered to the specified {@code FrameworkListener}s in the
	 * order they are specified.
	 * 
	 * <p>
	 * When this process completes after the bundles are refreshed, the
	 * Framework will fire a Framework event of type
	 * {@code FrameworkEvent.PACKAGES_REFRESHED} to announce it has completed
	 * the bundle refresh. The specified {@code FrameworkListener}s are notified
	 * in the order specified. Each specified {@code FrameworkListener} will be
	 * called with a Framework event of type
	 * {@code FrameworkEvent.PACKAGES_REFRESHED}.
	 * 
	 * @param bundles The bundles to be refreshed, or {@code null} to refresh
	 *        the {@link #getRemovalPendingBundles() removal pending} bundles.
	 * @param listeners Zero or more listeners to be notified when the bundle
	 *        refresh has been completed. The specified listeners do not need to
	 *        be otherwise registered with the framework. If a specified
	 *        listener is already registered with the framework, it will be
	 *        notified twice.
	 * @throws IllegalArgumentException If the specified {@code Bundle}s were
	 *         not created by the same framework instance associated with this
	 *         FrameworkWiring.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[System Bundle,RESOLVE]} and the Java
	 *         runtime environment supports permissions.
	 */
	void refreshBundles(Collection<Bundle> bundles,
			FrameworkListener... listeners);

	/**
	 * Resolves the specified bundles. The Framework must attempt to resolve the
	 * specified bundles that are unresolved. Additional bundles that are not
	 * included in the specified bundles may be resolved as a result of calling
	 * this method. A permissible implementation of this method is to attempt to
	 * resolve all unresolved bundles installed in the framework.
	 * 
	 * <p>
	 * If no bundles are specified, then the Framework will attempt to resolve
	 * all unresolved bundles. This method must not cause any bundle to be
	 * refreshed, stopped, or started. This method will not return until the
	 * operation has completed.
	 * 
	 * @param bundles The bundles to resolve or {@code null} to resolve all
	 *        unresolved bundles installed in the Framework.
	 * @return {@code true} if all specified bundles are resolved; {@code false}
	 *         otherwise.
	 * @throws IllegalArgumentException If the specified {@code Bundle}s were
	 *         not created by the same framework instance associated with this
	 *         FrameworkWiring.
	 * @throws SecurityException If the caller does not have
	 *         {@code AdminPermission[System Bundle,RESOLVE]} and the Java
	 *         runtime environment supports permissions.
	 */
	boolean resolveBundles(Collection<Bundle> bundles);

	/**
	 * Returns the bundles that have {@link BundleWiring#isCurrent()
	 * non-current}, {@link BundleWiring#isInUse() in use} bundle wirings. This
	 * is typically the bundles which have been updated or uninstalled since the
	 * last call to {@link #refreshBundles(Collection, FrameworkListener...)}.
	 * 
	 * @return A collection containing a snapshot of the {@code Bundle}s which
	 *         have non-current, in use {@code BundleWiring}s, or an empty
	 *         collection if there are no such bundles.
	 */
	Collection<Bundle> getRemovalPendingBundles();

	/**
	 * Returns the dependency closure for the specified bundles.
	 * 
	 * <p>
	 * A graph of bundles is computed starting with the specified bundles. The
	 * graph is expanded by adding any bundle that is either wired to a package
	 * that is currently exported by a bundle in the graph or requires a bundle
	 * in the graph. The graph is fully constructed when there is no bundle
	 * outside the graph that is wired to a bundle in the graph. The graph may
	 * contain {@code UNINSTALLED} bundles that are
	 * {@link #getRemovalPendingBundles() removal pending}.
	 * 
	 * @param bundles The initial bundles for which to generate the dependency
	 *        closure.
	 * @return A collection containing a snapshot of the dependency closure of
	 *         the specified bundles, or an empty collection if there were no
	 *         specified bundles.
	 * @throws IllegalArgumentException If the specified {@code Bundle}s were
	 *         not created by the same framework instance associated with this
	 *         FrameworkWiring.
	 */
	Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles);
}
