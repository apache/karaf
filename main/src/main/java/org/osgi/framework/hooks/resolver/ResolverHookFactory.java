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

package org.osgi.framework.hooks.resolver;

import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

/** 
 * OSGi Framework Resolver Hook Factory Service.
 * 
 * <p>
 * Bundles registering this service will be called by the framework during 
 * a bundle resolver process to obtain a {@link ResolverHook resolver hook}
 * instance which will be used for the duration of a resolve process.
 * 
 * @ThreadSafe
 * @see ResolverHook
 * @version $Id: 4023566367435f07c047a7ba571f3bedc53aa37a $
 */
public interface ResolverHookFactory {
	/**
	 * This method is called by the framework each time a resolve process begins
	 * to obtain a {@link ResolverHook resolver hook} instance.  This resolver hook 
	 * instance will be used for the duration of the resolve process.  At the end of 
	 * the resolve process the method {@link ResolverHook#end()} must be called by 
	 * the framework and the framework must not hold any references of the resolver 
	 * hook instance.
	 * <p>
	 * The triggers represent the collection of bundles which triggered
	 * the resolve process.  This collection may be empty if the triggers
	 * cannot be determined by the framework.  In most cases the triggers 
	 * can easily be determined.  Calling certain methods on 
	 * {@link Bundle bundle} when a bundle is in the {@link Bundle#INSTALLED INSTALLED} 
	 * state will cause the framework to begin a resolve process in order to resolve the 
	 * bundle.  The following methods will start a resolve process in this case:
	 * <ul>
	 *   <li>{@link Bundle#start() start}</li>
	 *   <li>{@link Bundle#loadClass(String) loadClass}</li>
	 *   <li>{@link Bundle#findEntries(String, String, boolean) findEntries}</li>
	 *   <li>{@link Bundle#getResource(String) getResource}</li>
	 *   <li>{@link Bundle#getResources(String) getResources}</li>
	 * </ul> 
	 * In such cases the collection will contain the single bundle which the
	 * framework is trying to resolve.  Other cases will cause multiple bundles to be
	 * included in the trigger bundles collection.  When {@link FrameworkWiring#resolveBundles(Collection)
	 * resolveBundles} is called the collection of triggers must include all the current bundle 
	 * revisions for bundles passed to resolveBundles which are in the {@link Bundle#INSTALLED INSTALLED}
	 * state.
	 * <p>
	 * When {@link FrameworkWiring#refreshBundles(Collection, org.osgi.framework.FrameworkListener...)}
	 * is called the collection of triggers is determined with the following steps:
	 * <ul>
	 *   <li>If the collection of bundles passed is null then {@link FrameworkWiring#getRemovalPendingBundles()}
	 *   is called to get the initial collection of bundles.</li>
	 *   <li>The equivalent of calling {@link FrameworkWiring#getDependencyClosure(Collection)} is called with
	 *   the initial collection of bundles to get the dependency closure collection of the bundles being refreshed.</li>
	 *   <li>Remove any non-active bundles from the dependency closure collection.</li>
	 *   <li>For each bundle remaining in the dependency closure collection get the current bundle revision
	 *   and add it to the collection of triggers.</li> 
	 * </ul>
	 * <p>
	 * As described above, a resolve process is typically initiated as a result of calling API that causes the 
	 * framework to attempt to resolve one or more bundles.  
	 * The framework is free to start a resolve process at any time for reasons other than calls to framework API.
	 * For example, a resolve process may be used by the framework for diagnostic purposes and result in no
	 * bundles actually becoming resolved at the end of the process.
	 * Resolver hook implementations must be prepared for resolve processes that are initiated for other reasons
	 * besides calls to framework API.
	 * @param triggers an unmodifiable collection of bundles which triggered the resolve process.
	 * This collection may be empty if the collection of trigger bundles cannot be
	 * determined.
	 * @return a resolver hook instance to be used for the duration of the resolve process.
	 * A {@code null} value may be returned which indicates this resolver hook factory abstains from
	 * the resolve process.
	 */
	ResolverHook begin(Collection<BundleRevision> triggers);
}
