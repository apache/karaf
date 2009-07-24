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
 * Registered implementations of this class are notified via {@link DmtEvent}
 * objects about important changes in the tree. Events are generated after every
 * successful DMT change, and also when sessions are opened or closed. If a
 * {@link DmtSession} is opened in atomic mode, DMT events are only sent when
 * the session is committed, when the changes are actually performed.
 * 
 * @version $Revision: 5673 $
 */
public interface DmtEventListener {

    /**
     * <code>DmtAdmin</code> uses this method to notify the registered
     * listeners about the change. This method is called asynchronously from the
     * actual event occurrence.
     * 
     * @param event the <code>DmtEvent</code> describing the change in detail
     */
    void changeOccurred(DmtEvent event);
}
