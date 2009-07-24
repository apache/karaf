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

package info.dmtree.spi;

import info.dmtree.DmtException;
import info.dmtree.DmtSession;

/**
 * An implementation of this interface takes the responsibility of handling data
 * requests in a subtree of the DMT.
 * <p>
 * In an OSGi environment such implementations should be registered at the OSGi
 * service registry specifying the list of root node URIs in a
 * <code>String</code> array in the <code>dataRootURIs</code> registration
 * parameter.
 * <p>
 * When the first reference in a session is made to a node handled by this
 * plugin, the DmtAdmin calls one of the <code>open...</code> methods to
 * retrieve a plugin session object for processing the request. The called
 * method depends on the lock type of the current session. In case of
 * {@link #openReadWriteSession(String[], DmtSession)} and
 * {@link #openAtomicSession(String[], DmtSession)}, the plugin may return
 * <code>null</code> to indicate that the specified lock type is not supported.
 * In this case the DmtAdmin may call
 * {@link #openReadOnlySession(String[], DmtSession)} to start a read-only
 * plugin session, which can be used as long as there are no write operations on
 * the nodes handled by this plugin.
 * <p>
 * The <code>sessionRoot</code> parameter of each method is a String array
 * containing the segments of the URI pointing to the root of the session. This
 * is an absolute path, so the first segment is always &quot;.&quot;. Special
 * characters appear escaped in the segments.
 * <p>
 * 
 * @version $Revision: 5673 $
 */
public interface DataPlugin {

    /**
     * This method is called to signal the start of a read-only session when the
     * first reference is made within a <code>DmtSession</code> to a node
     * which is handled by this plugin. Session information is given as it is
     * needed for sending alerts back from the plugin.
     * <p>
     * The plugin can assume that there are no writing sessions open on any
     * subtree that has any overlap with the subtree of this session.
     * 
     * @param sessionRoot the path to the subtree which is accessed in the
     *        current session, must not be <code>null</code>
     * @param session the session from which this plugin instance is accessed,
     *        must not be <code>null</code>
     * @return a plugin session capable of executing read operations
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>sessionRoot</code>
     *         points to a non-existing node
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if some underlying operation failed because of
     *         lack of permissions
     */
    ReadableDataSession openReadOnlySession(String[] sessionRoot,
            DmtSession session) throws DmtException;

    /**
     * This method is called to signal the start of a non-atomic read-write
     * session when the first reference is made within a <code>DmtSession</code>
     * to a node which is handled by this plugin. Session information is given
     * as it is needed for sending alerts back from the plugin.
     * <p>
     * The plugin can assume that there are no other sessions open on any
     * subtree that has any overlap with the subtree of this session.
     * 
     * @param sessionRoot the path to the subtree which is locked in the current
     *        session, must not be <code>null</code>
     * @param session the session from which this plugin instance is accessed,
     *        must not be <code>null</code>
     * @return a plugin session capable of executing read-write operations, or
     *         <code>null</code> if the plugin does not support non-atomic
     *         read-write sessions
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>sessionRoot</code>
     *         points to a non-existing node
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if some underlying operation failed because of
     *         lack of permissions
     */
    ReadWriteDataSession openReadWriteSession(String[] sessionRoot,
            DmtSession session) throws DmtException;

    /**
     * This method is called to signal the start of an atomic read-write session
     * when the first reference is made within a <code>DmtSession</code> to a
     * node which is handled by this plugin. Session information is given as it
     * is needed for sending alerts back from the plugin.
     * <p>
     * The plugin can assume that there are no other sessions open on any
     * subtree that has any overlap with the subtree of this session.
     * 
     * @param sessionRoot the path to the subtree which is locked in the current
     *        session, must not be <code>null</code>
     * @param session the session from which this plugin instance is accessed,
     *        must not be <code>null</code>
     * @return a plugin session capable of executing read-write operations in an
     *         atomic block, or <code>null</code> if the plugin does not
     *         support atomic read-write sessions
     * @throws DmtException with the following possible error codes:
     *         <ul>
     *         <li><code>NODE_NOT_FOUND</code> if <code>sessionRoot</code>
     *         points to a non-existing node
     *         <li><code>COMMAND_FAILED</code> if some unspecified error is
     *         encountered while attempting to complete the command
     *         </ul>
     * @throws SecurityException if some underlying operation failed because of
     *         lack of permissions
     */
    TransactionalDataSession openAtomicSession(String[] sessionRoot,
            DmtSession session) throws DmtException;
}
