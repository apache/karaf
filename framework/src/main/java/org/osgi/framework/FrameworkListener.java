/*
 * Copyright (c) OSGi Alliance (2000, 2008). All Rights Reserved.
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
 * A <code>FrameworkEvent</code> listener. <code>FrameworkListener</code> is
 * a listener interface that may be implemented by a bundle developer. When a
 * <code>FrameworkEvent</code> is fired, it is asynchronously delivered to a
 * <code>FrameworkListener</code>. The Framework delivers
 * <code>FrameworkEvent</code> objects to a <code>FrameworkListener</code>
 * in order and must not concurrently call a <code>FrameworkListener</code>.
 * 
 * <p>
 * A <code>FrameworkListener</code> object is registered with the Framework
 * using the {@link BundleContext#addFrameworkListener} method.
 * <code>FrameworkListener</code> objects are called with a
 * <code>FrameworkEvent</code> objects when the Framework starts and when
 * asynchronous errors occur.
 * 
 * @see FrameworkEvent
 * @NotThreadSafe
 * @version $Revision: 5673 $
 */

public interface FrameworkListener extends EventListener {

	/**
	 * Receives notification of a general <code>FrameworkEvent</code> object.
	 * 
	 * @param event The <code>FrameworkEvent</code> object.
	 */
	public void frameworkEvent(FrameworkEvent event);
}
