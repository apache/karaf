/*
 * Copyright (c) OSGi Alliance (2008, 2009). All Rights Reserved.
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
package org.osgi.service.blueprint.container;

import org.osgi.framework.Bundle;

/**
 * A Blueprint Event.
 * 
 * <p>
 * <code>BlueprintEvent</code> objects are delivered to all registered
 * {@link BlueprintListener} services. Blueprint Events must be asynchronously
 * delivered in chronological order with respect to each listener.
 * 
 * <p>
 * In addition, after a Blueprint Listener is registered, the Blueprint extender
 * will synchronously send to this Blueprint Listener the last Blueprint Event
 * for each ready Blueprint bundle managed by this extender. This
 * <em>replay</em> of Blueprint Events is designed so that the new Blueprint
 * Listener can be informed of the state of each Blueprint bundle. Blueprint
 * Events sent during this replay will have the {@link #isReplay()} flag set.
 * The Blueprint extender must ensure that this replay phase does not interfere
 * with new Blueprint Events so that the chronological order of all Blueprint
 * Events received by the Blueprint Listener is preserved. If the last Blueprint
 * Event for a given Blueprint bundle is {@link #DESTROYED}, the extender must
 * not send it during this replay phase.
 * 
 * <p>
 * A type code is used to identify the type of event. The following event types
 * are defined:
 * <ul>
 * <li>{@link #CREATING}</li>
 * <li>{@link #CREATED}</li>
 * <li>{@link #DESTROYING}</li>
 * <li>{@link #DESTROYED}</li>
 * <li>{@link #FAILURE}</li>
 * <li>{@link #GRACE_PERIOD}</li>
 * <li>{@link #WAITING}</li>
 * </ul>
 * 
 * <p>
 * In addition to calling the registered {@link BlueprintListener} services, the
 * Blueprint extender must also send those events to the Event Admin service, if
 * it is available.
 * 
 * @see BlueprintListener
 * @see EventConstants
 * @Immutable
 * @version $Revision: 7590 $
 */
public class BlueprintEvent {

	/**
	 * The Blueprint extender has started creating a Blueprint Container for the
	 * bundle.
	 */
	public static final int	CREATING		= 1;
	/**
	 * The Blueprint extender has created a Blueprint Container for the bundle.
	 * This event is sent after the Blueprint Container has been registered as a
	 * service.
	 */
	public static final int	CREATED			= 2;
	/**
	 * The Blueprint extender has started destroying the Blueprint Container for
	 * the bundle.
	 */
	public static final int	DESTROYING		= 3;
	/**
	 * The Blueprint Container for the bundle has been completely destroyed.
	 * This event is sent after the Blueprint Container has been unregistered as
	 * a service.
	 */
	public static final int	DESTROYED		= 4;
	/**
	 * The Blueprint Container creation for the bundle has failed. If this event
	 * is sent after a timeout in the Grace Period, the
	 * {@link #getDependencies()} method must return an array of missing
	 * mandatory dependencies. The event must also contain the cause of the
	 * failure as a <code>Throwable</code> through the {@link #getCause()}
	 * method.
	 */
	public static final int	FAILURE			= 5;
	/**
	 * The Blueprint Container has entered the grace period. The list of missing
	 * dependencies must be made available through the
	 * {@link #getDependencies()} method. During the grace period, a
	 * {@link #GRACE_PERIOD} event is sent each time the set of unsatisfied
	 * dependencies changes.
	 */
	public static final int	GRACE_PERIOD	= 6;
	/**
	 * The Blueprint Container is waiting on the availability of a service to
	 * satisfy an invocation on a referenced service. The missing dependency
	 * must be made available through the {@link #getDependencies()} method
	 * which will return an array containing one filter object as a String.
	 */
	public static final int	WAITING			= 7;

	/**
	 * Type of this event.
	 * 
	 * @see #getType()
	 */
	private final int		type;
	/**
	 * The time when the event occurred.
	 * 
	 * @see #getTimestamp()
	 */
	private final long		timestamp;
	/**
	 * The Blueprint bundle.
	 * 
	 * @see #getBundle()
	 */
	private final Bundle	bundle;
	/**
	 * The Blueprint extender bundle.
	 * 
	 * @see #getExtenderBundle()
	 */
	private final Bundle	extenderBundle;
	/**
	 * An array containing filters identifying the missing dependencies. Must
	 * not be <code>null</code> when the event type requires it.
	 * 
	 * @see #getDependencies()
	 */
	private final String[]	dependencies;
	/**
	 * Cause of the failure.
	 * 
	 * @see #getCause()
	 */
	private final Throwable	cause;
	/**
	 * Indicate if this event is a replay event or not.
	 * 
	 * @see #isReplay()
	 */
	private final boolean	replay;

	/**
	 * Create a simple <code>BlueprintEvent</code> object.
	 * 
	 * @param type The type of this event.
	 * @param bundle The Blueprint bundle associated with this event. This
	 *        parameter must not be <code>null</code>.
	 * @param extenderBundle The Blueprint extender bundle that is generating
	 *        this event. This parameter must not be <code>null</code>.
	 */
	public BlueprintEvent(int type, Bundle bundle, Bundle extenderBundle) {
		this(type, bundle, extenderBundle, null, null);
	}

	/**
	 * Create a <code>BlueprintEvent</code> object associated with a set of
	 * dependencies.
	 * 
	 * @param type The type of this event.
	 * @param bundle The Blueprint bundle associated with this event. This
	 *        parameter must not be <code>null</code>.
	 * @param extenderBundle The Blueprint extender bundle that is generating
	 *        this event. This parameter must not be <code>null</code>.
	 * @param dependencies An array of <code>String</code> filters for each
	 *        dependency associated with this event. Must be a non-empty array
	 *        for event types {@link #GRACE_PERIOD} and {@link #WAITING}. It is
	 *        optional for event type {@link #FAILURE}. Must be
	 *        <code>null</code> for other event types.
	 */
	public BlueprintEvent(int type, Bundle bundle, Bundle extenderBundle,
			String[] dependencies) {
		this(type, bundle, extenderBundle, dependencies, null);
	}

	/**
	 * Create a <code>BlueprintEvent</code> object associated with a failure
	 * cause.
	 * 
	 * @param type The type of this event.
	 * @param bundle The Blueprint bundle associated with this event. This
	 *        parameter must not be <code>null</code>.
	 * @param extenderBundle The Blueprint extender bundle that is generating
	 *        this event. This parameter must not be <code>null</code>.
	 * @param cause A <code>Throwable</code> object describing the root cause of
	 *        the event. May be <code>null</code>.
	 */
	public BlueprintEvent(int type, Bundle bundle, Bundle extenderBundle,
			Throwable cause) {
		this(type, bundle, extenderBundle, null, cause);
	}

	/**
	 * Create a <code>BlueprintEvent</code> object associated with a failure
	 * cause and a set of dependencies.
	 * 
	 * @param type The type of this event.
	 * @param bundle The Blueprint bundle associated with this event. This
	 *        parameter must not be <code>null</code>.
	 * @param extenderBundle The Blueprint extender bundle that is generating
	 *        this event. This parameter must not be <code>null</code>.
	 * @param dependencies An array of <code>String</code> filters for each
	 *        dependency associated with this event. Must be a non-empty array
	 *        for event types {@link #GRACE_PERIOD} and {@link #WAITING}. It is
	 *        optional for event type {@link #FAILURE}. Must be
	 *        <code>null</code> for other event types.
	 * @param cause A <code>Throwable</code> object describing the root cause of
	 *        this event. May be <code>null</code>.
	 */
	public BlueprintEvent(int type, Bundle bundle, Bundle extenderBundle,
			String[] dependencies, Throwable cause) {
		this.type = type;
		this.timestamp = System.currentTimeMillis();
		this.bundle = bundle;
		this.extenderBundle = extenderBundle;
		this.dependencies = dependencies;
		this.cause = cause;
		this.replay = false;
		if (bundle == null) {
			throw new NullPointerException("bundle must not be null");
		}
		if (extenderBundle == null) {
			throw new NullPointerException("extenderBundle must not be null");
		}
		switch (type) {
			case WAITING :
			case GRACE_PERIOD :
				if (dependencies == null) {
					throw new NullPointerException(
							"dependencies must not be null");
				}
				if (dependencies.length == 0) {
					throw new IllegalArgumentException(
							"dependencies must not be length zero");
				}
				break;
			case FAILURE :
				if ((dependencies != null) && (dependencies.length == 0)) {
					throw new IllegalArgumentException(
							"dependencies must not be length zero");
				}
				break;
			default :
				if (dependencies != null) {
					throw new IllegalArgumentException(
							"dependencies must be null");
				}
				break;
		}
	}

	/**
	 * Create a new <code>BlueprintEvent</code> from the specified
	 * <code>BlueprintEvent</code>. The <code>timestamp</code> property will be
	 * copied from the original event and only the replay property will be
	 * overridden with the given value.
	 * 
	 * @param event The original <code>BlueprintEvent</code> to copy. Must not
	 *        be <code>null</code>.
	 * @param replay <code>true</code> if this event should be used as a replay
	 *        event.
	 */
	public BlueprintEvent(BlueprintEvent event, boolean replay) {
		this.type = event.type;
		this.timestamp = event.timestamp;
		this.bundle = event.bundle;
		this.extenderBundle = event.extenderBundle;
		this.dependencies = event.dependencies;
		this.cause = event.cause;
		this.replay = replay;
	}

	/**
	 * Return the type of this event.
	 * <p>
	 * The type values are:
	 * <ul>
	 * <li>{@link #CREATING}</li>
	 * <li>{@link #CREATED}</li>
	 * <li>{@link #DESTROYING}</li>
	 * <li>{@link #DESTROYED}</li>
	 * <li>{@link #FAILURE}</li>
	 * <li>{@link #GRACE_PERIOD}</li>
	 * <li>{@link #WAITING}</li>
	 * </ul>
	 * 
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Return the time at which this event was created.
	 * 
	 * @return The time at which this event was created.
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Return the Blueprint bundle associated with this event.
	 * 
	 * @return The Blueprint bundle associated with this event.
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * Return the Blueprint extender bundle that is generating this event.
	 * 
	 * @return The Blueprint extender bundle that is generating this event.
	 */
	public Bundle getExtenderBundle() {
		return extenderBundle;
	}

	/**
	 * Return the filters identifying the missing dependencies that caused this
	 * event.
	 * 
	 * @return The filters identifying the missing dependencies that caused this
	 *         event if the event type is one of {@link #WAITING},
	 *         {@link #GRACE_PERIOD} or {@link #FAILURE} or <code>null</code>
	 *         for the other event types.
	 */
	public String[] getDependencies() {
		return dependencies == null ? null : (String[]) dependencies.clone();
	}

	/**
	 * Return the cause for this {@link #FAILURE} event.
	 * 
	 * @return The cause of the failure for this event. May be <code>null</code>
	 *         .
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * Return whether this event is a replay event.
	 * 
	 * @return <code>true</code> if this event is a replay event and
	 *         <code>false</code> otherwise.
	 */
	public boolean isReplay() {
		return replay;
	}
}
