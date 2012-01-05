/*
 * Copyright (c) OSGi Alliance (2000, 2010). All Rights Reserved.
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

package org.osgi.framework;

import java.util.EventListener;

/**
 * A {@code BundleEvent} listener. {@code BundleListener} is a
 * listener interface that may be implemented by a bundle developer. When a
 * {@code BundleEvent} is fired, it is asynchronously delivered to a
 * {@code BundleListener}. The Framework delivers
 * {@code BundleEvent} objects to a {@code BundleListener} in
 * order and must not concurrently call a {@code BundleListener}.
 * <p>
 * A {@code BundleListener} object is registered with the Framework using
 * the {@link BundleContext#addBundleListener} method.
 * {@code BundleListener}s are called with a {@code BundleEvent}
 * object when a bundle has been installed, resolved, started, stopped, updated,
 * unresolved, or uninstalled.
 * 
 * @see BundleEvent
 * @NotThreadSafe
 * @version $Id: 77cdaebd3ac97c6798fc3043957abd1bd6d01ccb $
 */

public interface BundleListener extends EventListener {
	/**
	 * Receives notification that a bundle has had a lifecycle change.
	 * 
	 * @param event The {@code BundleEvent}.
	 */
	public void bundleChanged(BundleEvent event);
}
