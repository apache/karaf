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

/**
 * Event property names used in Event Admin events published by a Blueprint
 * Container.
 * 
 * <p>
 * Each type of event is sent to a different topic:
 * 
 * <p>
 * <code>org/osgi/service/blueprint/container/</code><em>&lt;event-type&gt;</em>
 * 
 * <p>
 * where <em>&lt;event-type&gt;</em> can have the values
 * {@link BlueprintEvent#CREATING CREATING}, {@link BlueprintEvent#CREATED
 * CREATED}, {@link BlueprintEvent#DESTROYING DESTROYING},
 * {@link BlueprintEvent#DESTROYED DESTROYED}, {@link BlueprintEvent#FAILURE
 * FAILURE}, {@link BlueprintEvent#GRACE_PERIOD GRACE_PERIOD}, or
 * {@link BlueprintEvent#WAITING WAITING}.
 * 
 * <p>
 * Such events have the following properties:
 * <ul>
 * <li>{@link #TYPE type}</li>
 * <li>{@link #EVENT event}</li>
 * <li>{@link #TIMESTAMP timestamp}</li>
 * <li>{@link #BUNDLE bundle}</li>
 * <li>{@link #BUNDLE_SYMBOLICNAME bundle.symbolicName}</li>
 * <li>{@link #BUNDLE_ID bundle.id}</li>
 * <li>{@link #BUNDLE_VERSION bundle.version}</li>
 * <li>{@link #EXTENDER_BUNDLE_SYMBOLICNAME extender.bundle.symbolicName}</li>
 * <li>{@link #EXTENDER_BUNDLE_ID extender.bundle.id}</li>
 * <li>{@link #EXTENDER_BUNDLE_VERSION extender.bundle.version}</li>
 * <li>{@link #DEPENDENCIES dependencies}</li>
 * <li>{@link #CAUSE cause}</li>
 * </ul>
 * 
 * @Immutable
 * @version $Revision: 7564 $
 */
public class EventConstants {
	private EventConstants() {
		// non-instantiable class
	}

	/**
	 * The type of the event that has been issued. This property is of type
	 * <code>Integer</code> and can take one of the values defined in
	 * {@link BlueprintEvent}.
	 */
	public static final String TYPE = "type";

	/**
	 * The <code>BlueprintEvent</code> object that caused this event. This
	 * property is of type {@link BlueprintEvent}.
	 */
	public static final String EVENT = "event";

	/**
	 * The time the event was created. This property is of type
	 * <code>Long</code>.
	 */
	public static final String TIMESTAMP = "timestamp";

	/**
	 * The Blueprint bundle associated with this event. This property is of type
	 * <code>Bundle</code>.
	 */
	public static final String BUNDLE = "bundle";

	/**
	 * The bundle id of the Blueprint bundle associated with this event. This
	 * property is of type <code>Long</code>.
	 */
	public static final String BUNDLE_ID = "bundle.id";

	/**
	 * The bundle symbolic name of the Blueprint bundle associated with this
	 * event. This property is of type <code>String</code>.
	 */
	public static final String BUNDLE_SYMBOLICNAME = "bundle.symbolicName";

	/**
	 * The bundle version of the Blueprint bundle associated with this event.
	 * This property is of type <code>Version</code>.
	 */
	public static final String BUNDLE_VERSION = "bundle.version";

	/**
	 * The Blueprint extender bundle that is generating this event. This
	 * property is of type <code>Bundle</code>.
	 */
	public static final String EXTENDER_BUNDLE = "extender.bundle";

	/**
	 * The bundle id of the Blueprint extender bundle that is generating this
	 * event. This property is of type <code>Long</code>.
	 */
	public static final String EXTENDER_BUNDLE_ID = "extender.bundle.id";

	/**
	 * The bundle symbolic of the Blueprint extender bundle that is generating
	 * this event. This property is of type <code>String</code>.
	 */
	public static final String EXTENDER_BUNDLE_SYMBOLICNAME = "extender.bundle.symbolicName";

	/**
	 * The bundle version of the Blueprint extender bundle that is generating
	 * this event. This property is of type <code>Version</code>.
	 */
	public static final String EXTENDER_BUNDLE_VERSION = "extender.bundle.version";

	/**
	 * The filters identifying the missing dependencies that caused this event
	 * for a {@link BlueprintEvent#FAILURE FAILURE},
	 * {@link BlueprintEvent#GRACE_PERIOD GRACE_PERIOD}, or
	 * {@link BlueprintEvent#WAITING WAITING} event. This property type is an
	 * array of <code>String</code>.
	 */
	public static final String DEPENDENCIES = "dependencies";

	/**
	 * The cause for a {@link BlueprintEvent#FAILURE FAILURE} event. This
	 * property is of type <code>Throwable</code>.
	 */
	public static final String CAUSE = "cause";

	/**
	 * Topic prefix for all events issued by the Blueprint Container
	 */
	public static final String TOPIC_BLUEPRINT_EVENTS = "org/osgi/service/blueprint/container";

	/**
	 * Topic for Blueprint Container CREATING events
	 */
	public static final String TOPIC_CREATING = TOPIC_BLUEPRINT_EVENTS
			+ "/CREATING";

	/**
	 * Topic for Blueprint Container CREATED events
	 */
	public static final String TOPIC_CREATED = TOPIC_BLUEPRINT_EVENTS
			+ "/CREATED";

	/**
	 * Topic for Blueprint Container DESTROYING events
	 */
	public static final String TOPIC_DESTROYING = TOPIC_BLUEPRINT_EVENTS
			+ "/DESTROYING";

	/**
	 * Topic for Blueprint Container DESTROYED events
	 */
	public static final String TOPIC_DESTROYED = TOPIC_BLUEPRINT_EVENTS
			+ "/DESTROYED";

	/**
	 * Topic for Blueprint Container FAILURE events
	 */
	public static final String TOPIC_FAILURE = TOPIC_BLUEPRINT_EVENTS
			+ "/FAILURE";

	/**
	 * Topic for Blueprint Container GRACE_PERIOD events
	 */
	public static final String TOPIC_GRACE_PERIOD = TOPIC_BLUEPRINT_EVENTS
			+ "/GRACE_PERIOD";

	/**
	 * Topic for Blueprint Container WAITING events
	 */
	public static final String TOPIC_WAITING = TOPIC_BLUEPRINT_EVENTS
			+ "/WAITING";
}
