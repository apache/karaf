/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.servicebinder;

/**
 * This class is the event generated when the availability of the
 * underlying object associated with an <tt>InstanceReference</tt>
 * changes. Use the <tt>InstanceReferenceListener</tt> interface
 * to listen for this event. The <tt>getSource()</tt> method
 * returns the <tt>InstanceReference</tt> that generated the event.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
**/
public class InstanceReferenceEvent extends java.util.EventObject
{
    private static final long serialVersionUID = 189791898139565080L;

    /**
     * Construct an event with the specified source instance reference.
     * @param ir the instance reference that generated the event.
    **/
    public InstanceReferenceEvent(InstanceReference ir)
    {
        super(ir);
    }
}
