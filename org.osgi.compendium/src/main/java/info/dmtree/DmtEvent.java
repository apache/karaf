/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
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
package info.dmtree;

/**
 * Event class storing the details of a change in the tree.
 * <code>DmtEvent</code> is used by <code>DmtAdmin</code> to notify registered
 * {@link DmtEventListener EventListeners} about important changes. Events are
 * generated after every successful DMT change, and also when sessions are
 * opened or closed. If a {@link DmtSession} is opened in atomic mode, DMT
 * events are only sent when the session is committed, when the changes are
 * actually performed.
 * <p>
 * An event is generated for each group of nodes added, deleted, replaced,
 * renamed or copied, in this order. Events are also generated when sessions are
 * opened and closed.
 * <p>
 * The <code>type</code> of the event describes the change that triggered the
 * event delivery. Each event carries the unique identifier of the session in
 * which the described change happened. The events describing changes in the DMT
 * carry the list of affected nodes. In case of {@link #COPIED} or
 * {@link #RENAMED} events, the event carries the list of new nodes as well.
 * <p>
 * When a <code>DmtEvent</code> is delivered to a listener, the event contains
 * only those node URIs that the listener has access to. This access control
 * decision is based on the principal specified when the listener was
 * registered:
 * <ul>
 * <li>If the listener was registered specifying an explicit principal, using
 * the {@link DmtAdmin#addEventListener(String, int, String, DmtEventListener)}
 * method, then the target node ACLs should be checked for providing GET access
 * to the specified principal;
 * <li>When the listener was registered without an explicit principal then the
 * listener needs GET {@link info.dmtree.security.DmtPermission} for the
 * corresponding node.
 * </ul>
 * 
 * @version $Revision: 5673 $
 */
public interface DmtEvent {

    /**
     * Event type indicating nodes that were added.
     */
    int ADDED = 0x01;

    /**
     * Event type indicating nodes that were copied.
     */
    int COPIED = 0x02;

    /**
     * Event type indicating nodes that were deleted.
     */
    int DELETED = 0x04;

    /**
     * Event type indicating nodes that were renamed.
     */
    int RENAMED = 0x08;

    /**
     * Event type indicating nodes that were replaced.
     */
    int REPLACED = 0x10;

    /**
     * Event type indicating that a new session was opened.
     */
    int SESSION_OPENED = 0x20;

    /**
     * Event type indicating that a session was closed.  This type of event is
     * sent when the session is closed by the client or becomes inactive for any
     * other reason (session timeout, fatal errors in business methods, etc.).
     */
    int SESSION_CLOSED = 0x40;

    /**
     * This method returns the type of this event.
     * 
     * @return the type of this event.
     */
    int getType();

    /**
     * This method returns the identifier of the session in which this event
     * took place. The ID is guaranteed to be unique on a machine.
     * 
     * @return the unique indetifier of the session that triggered the event
     */
    int getSessionId();

    /**
     * This method can be used to query the subject nodes of this event. The
     * method returns <code>null</code> for {@link #SESSION_OPENED} and
     * {@link #SESSION_CLOSED}.
     * <p>
     * The method returns only those affected nodes that the caller has the GET
     * permission for (or in case of {@link #COPIED} or {@link #RENAMED} events,
     * where the caller has GET permissions for either the source or the
     * destination nodes). Therefore, it is possible that the method returns an
     * empty array. All returned URIs are absolute.
     * 
     * @return the array of affected nodes
     * @see #getNewNodes
     */
    String[] getNodes();

    /**
     * This method can be used to query the new nodes, when the type of the
     * event is {@link #COPIED} or {@link #RENAMED}. For all other event types
     * this method returns <code>null</code>.
     * <p>
     * The array returned by this method runs parallel to the array returned by
     * {@link #getNodes}, the elements in the two arrays contain the source and
     * destination URIs for the renamed or copied nodes in the same order. All
     * returned URIs are absolute.
     * <p>
     * This method returns only those nodes where the caller has the GET
     * permission for the source or destination node of the operation.
     * Therefore, it is possible that the method returns an empty array.
     * 
     * @return the array of newly created nodes
     */
    String[] getNewNodes();
}
