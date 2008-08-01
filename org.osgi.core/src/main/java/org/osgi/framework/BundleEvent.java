/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/BundleEvent.java,v 1.19 2007/02/20 00:14:12 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2000, 2007). All Rights Reserved.
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
 * An event from the Framework describing a bundle lifecycle change.
 * <p>
 * <code>BundleEvent</code> objects are delivered to
 * <code>SynchronousBundleListener</code>s and <code>BundleListener</code>s
 * when a change occurs in a bundle's lifecycle. A type code is used to identify
 * the event type for future extendability.
 * 
 * <p>
 * OSGi Alliance reserves the right to extend the set of types.
 * 
 * @Immutable
 * @see BundleListener
 * @see SynchronousBundleListener
 * @version $Revision: 1.19 $
 */

public class BundleEvent extends EventObject {
	static final long		serialVersionUID	= 4080640865971756012L;
	/**
	 * Bundle that had a change occur in its lifecycle.
	 */
	private final Bundle	bundle;

	/**
	 * Type of bundle lifecycle change.
	 */
	private final int		type;

	/**
	 * The bundle has been installed.
	 * <p>
	 * The value of <code>INSTALLED</code> is 0x00000001.
	 * 
	 * @see BundleContext#installBundle(String)
	 */
	public final static int	INSTALLED			= 0x00000001;

	/**
	 * The bundle has been started.
	 * <p>
	 * The bundle's
	 * {@link BundleActivator#start(BundleContext) BundleActivator start} method
	 * has been executed if the bundle has a bundle activator class.
	 * <p>
	 * The value of <code>STARTED</code> is 0x00000002.
	 * 
	 * @see Bundle#start()
	 */
	public final static int	STARTED				= 0x00000002;

	/**
	 * The bundle has been stopped.
	 * <p>
	 * The bundle's
	 * {@link BundleActivator#stop(BundleContext) BundleActivator stop} method
	 * has been executed if the bundle has a bundle activator class.
	 * <p>
	 * The value of <code>STOPPED</code> is 0x00000004.
	 * 
	 * @see Bundle#stop()
	 */
	public final static int	STOPPED				= 0x00000004;

	/**
	 * The bundle has been updated.
	 * <p>
	 * The value of <code>UPDATED</code> is 0x00000008.
	 * 
	 * @see Bundle#update()
	 */
	public final static int	UPDATED				= 0x00000008;

	/**
	 * The bundle has been uninstalled.
	 * <p>
	 * The value of <code>UNINSTALLED</code> is 0x00000010.
	 * 
	 * @see Bundle#uninstall
	 */
	public final static int	UNINSTALLED			= 0x00000010;

	/**
	 * The bundle has been resolved.
	 * <p>
	 * The value of <code>RESOLVED</code> is 0x00000020.
	 * 
	 * @see Bundle#RESOLVED
	 * @since 1.3
	 */
	public final static int	RESOLVED			= 0x00000020;

	/**
	 * The bundle has been unresolved.
	 * <p>
	 * The value of <code>UNRESOLVED</code> is 0x00000040.
	 * 
	 * @see Bundle#INSTALLED
	 * @since 1.3
	 */
	public final static int	UNRESOLVED			= 0x00000040;

	/**
	 * The bundle is about to be activated.
	 * <p>
	 * The bundle's
	 * {@link BundleActivator#start(BundleContext) BundleActivator start} method
	 * is about to be called if the bundle has a bundle activator class. This
	 * event is only delivered to {@link SynchronousBundleListener}s. It is not
	 * delivered to <code>BundleListener</code>s.
	 * <p>
	 * The value of <code>STARTING</code> is 0x00000080.
	 * 
	 * @see Bundle#start()
	 * @since 1.3
	 */
	public final static int	STARTING			= 0x00000080;

	/**
	 * The bundle is about to deactivated.
	 * <p>
	 * The bundle's
	 * {@link BundleActivator#stop(BundleContext) BundleActivator stop} method
	 * is about to be called if the bundle has a bundle activator class. This
	 * event is only delivered to {@link SynchronousBundleListener}s. It is not
	 * delivered to <code>BundleListener</code>s.
	 * <p>
	 * The value of <code>STOPPING</code> is 0x00000100.
	 * 
	 * @see Bundle#stop()
	 * @since 1.3
	 */
	public final static int	STOPPING			= 0x00000100;

	/**
	 * The bundle will be lazily activated.
	 * <p>
	 * The bundle has a {@link Constants#ACTIVATION_LAZY lazy activation policy}
	 * and is waiting to be activated. It is now in the
	 * {@link Bundle#STARTING STARTING} state and has a valid
	 * <code>BundleContext</code>. This event is only delivered to
	 * {@link SynchronousBundleListener}s. It is not delivered to
	 * <code>BundleListener</code>s.
	 * <p>
	 * The value of <code>LAZY_ACTIVATION</code> is 0x00000200.
	 * 
	 * @since 1.4
	 */
	public final static int	LAZY_ACTIVATION		= 0x00000200;

	/**
	 * Creates a bundle event of the specified type.
	 * 
	 * @param type The event type.
	 * @param bundle The bundle which had a lifecycle change.
	 */

	public BundleEvent(int type, Bundle bundle) {
		super(bundle);
		this.bundle = bundle;
		this.type = type;
	}

	/**
	 * Returns the bundle which had a lifecycle change. This bundle is the
	 * source of the event.
	 * 
	 * @return The bundle that had a change occur in its lifecycle.
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * Returns the type of lifecyle event. The type values are:
	 * <ul>
	 * <li>{@link #INSTALLED}
	 * <li>{@link #RESOLVED}
	 * <li>{@link #LAZY_ACTIVATION}
	 * <li>{@link #STARTING}
	 * <li>{@link #STARTED}
	 * <li>{@link #STOPPING}
	 * <li>{@link #STOPPED}
	 * <li>{@link #UPDATED}
	 * <li>{@link #UNRESOLVED}
	 * <li>{@link #UNINSTALLED}
	 * </ul>
	 * 
	 * @return The type of lifecycle event.
	 */

	public int getType() {
		return type;
	}
}
