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
 * A <code>BlueprintEvent</code> Listener.
 * 
 * <p>
 * To receive Blueprint Events, a bundle must register a Blueprint Listener
 * service.
 * 
 * After a Blueprint Listener is registered, the Blueprint extender must
 * synchronously send to this Blueprint Listener the last Blueprint Event for
 * each ready Blueprint bundle managed by this extender. This replay of
 * Blueprint Events is designed so that the new Blueprint Listener can be
 * informed of the state of each Blueprint bundle. Blueprint Events sent during
 * this replay will have the {@link BlueprintEvent#isReplay() isReplay()} flag
 * set. The Blueprint extender must ensure that this replay phase does not
 * interfere with new Blueprint Events so that the chronological order of all
 * Blueprint Events received by the Blueprint Listener is preserved. If the last
 * Blueprint Event for a given Blueprint bundle is
 * {@link BlueprintEvent#DESTROYED DESTROYED}, the extender must not send it
 * during this replay phase.
 * 
 * @see BlueprintEvent
 * @ThreadSafe
 * @version $Revision: 7564 $
 */
public interface BlueprintListener {

	/**
	 * Receives notifications of a Blueprint Event.
	 * 
	 * Implementers should quickly process the event and return.
	 * 
	 * @param event The {@link BlueprintEvent}.
	 */
	void blueprintEvent(BlueprintEvent event);
}
