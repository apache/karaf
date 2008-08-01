/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/FrameworkEvent.java,v 1.15 2007/02/20 00:14:12 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2004, 2007). All Rights Reserved.
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

import java.util.EventObject;

/**
 * A general event from the Framework.
 * 
 * <p>
 * <code>FrameworkEvent</code> objects are delivered to
 * <code>FrameworkListener</code>s when a general event occurs within the
 * OSGi environment. A type code is used to identify the event type for future
 * extendability.
 * 
 * <p>
 * OSGi Alliance reserves the right to extend the set of event types.
 * 
 * @Immutable
 * @see FrameworkListener
 * @version $Revision: 1.15 $
 */

public class FrameworkEvent extends EventObject {
	static final long		serialVersionUID	= 207051004521261705L;
	/**
	 * Bundle related to the event.
	 */
	private final Bundle	bundle;

	/**
	 * Exception related to the event.
	 */
	private final Throwable	throwable;

	/**
	 * Type of event.
	 */
	private final int		type;

	/**
	 * The Framework has started.
	 * 
	 * <p>
	 * This event is fired when the Framework has started after all installed
	 * bundles that are marked to be started have been started and the Framework
	 * has reached the intitial start level.
	 * 
	 * <p>
	 * The value of <code>STARTED</code> is 0x00000001.
	 * 
	 * @see "<code>StartLevel</code>"
	 */
	public final static int	STARTED				= 0x00000001;

	/**
	 * An error has occurred.
	 * 
	 * <p>
	 * There was an error associated with a bundle.
	 * 
	 * <p>
	 * The value of <code>ERROR</code> is 0x00000002.
	 */
	public final static int	ERROR				= 0x00000002;

	/**
	 * A PackageAdmin.refreshPackage operation has completed.
	 * 
	 * <p>
	 * This event is fired when the Framework has completed the refresh packages
	 * operation initiated by a call to the PackageAdmin.refreshPackages method.
	 * 
	 * <p>
	 * The value of <code>PACKAGES_REFRESHED</code> is 0x00000004.
	 * 
	 * @since 1.2
	 * @see "<code>PackageAdmin.refreshPackages</code>"
	 */
	public final static int	PACKAGES_REFRESHED	= 0x00000004;

	/**
	 * A StartLevel.setStartLevel operation has completed.
	 * 
	 * <p>
	 * This event is fired when the Framework has completed changing the active
	 * start level initiated by a call to the StartLevel.setStartLevel method.
	 * 
	 * <p>
	 * The value of <code>STARTLEVEL_CHANGED</code> is 0x00000008.
	 * 
	 * @since 1.2
	 * @see "<code>StartLevel</code>"
	 */
	public final static int	STARTLEVEL_CHANGED	= 0x00000008;

	/**
	 * A warning has occurred.
	 * 
	 * <p>
	 * There was a warning associated with a bundle.
	 * 
	 * <p>
	 * The value of <code>WARNING</code> is 0x00000010.
	 * 
	 * @since 1.3
	 */
	public final static int	WARNING				= 0x00000010;

	/**
	 * An informational event has occurred.
	 * 
	 * <p>
	 * There was an informational event associated with a bundle.
	 * 
	 * <p>
	 * The value of <code>INFO</code> is 0x00000020.
	 * 
	 * @since 1.3
	 */
	public final static int	INFO				= 0x00000020;

	/**
	 * Creates a Framework event.
	 * 
	 * @param type The event type.
	 * @param source The event source object. This may not be <code>null</code>.
	 * @deprecated As of 1.2. This constructor is deprecated in favor of using
	 *             the other constructor with the System Bundle as the event
	 *             source.
	 */
	public FrameworkEvent(int type, Object source) {
		super(source);
		this.type = type;
		this.bundle = null;
		this.throwable = null;
	}

	/**
	 * Creates a Framework event regarding the specified bundle.
	 * 
	 * @param type The event type.
	 * @param bundle The event source.
	 * @param throwable The related exception. This argument may be
	 *        <code>null</code> if there is no related exception.
	 */
	public FrameworkEvent(int type, Bundle bundle, Throwable throwable) {
		super(bundle);
		this.type = type;
		this.bundle = bundle;
		this.throwable = throwable;
	}

	/**
	 * Returns the exception related to this event.
	 * 
	 * @return The related exception or <code>null</code> if none.
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * Returns the bundle associated with the event. This bundle is also the
	 * source of the event.
	 * 
	 * @return The bundle associated with the event.
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * Returns the type of framework event.
	 * <p>
	 * The type values are:
	 * <ul>
	 * <li>{@link #STARTED}
	 * <li>{@link #ERROR}
	 * <li>{@link #WARNING}
	 * <li>{@link #INFO}
	 * <li>{@link #PACKAGES_REFRESHED}
	 * <li>{@link #STARTLEVEL_CHANGED}
	 * </ul>
	 * 
	 * @return The type of state change.
	 */

	public int getType() {
		return type;
	}
}
