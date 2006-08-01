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
 * This is an event listener for listening to changes in
 * the availability of the underlying object associated
 * with an <tt>InstanceReference</tt>. For the precise
 * details of when this event is fired, refer to the
 * methods below.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
**/
public interface InstanceReferenceListener extends java.util.EventListener
{
    /**
     * This method is called when an <tt>InstanceReference</tt>'s
     * underlying object becomes valid, i.e., the instance is
     * available for use. This event is fired during the following
     * sequence of steps:
     * <p>
     * <ol>
     *   <li>Instance created.</li>
     *   <li>Dependencies bound, if any.</li>
     *   <li>Services registered, if any.</li>
     *   <li><tt>Lifecycle.activate()</tt> is called, if the instance
     *       implements the <tt>Lifecycle</tt> interface.</li>
     *   <li>Fire <tt>InstanceReferenceListener.validated()</tt>.
     * </ol>
     * @param event the associated instance reference event.
    **/
    public void validated(InstanceReferenceEvent event);

    /**
     * This method is called when an <tt>InstanceReference</tt>'s
     * underlying object is going to be invalidated. This event
     * is fired during the following sequence of steps:
     * <p>
     * <ol>
     *   <li>Fire <tt>InstanceReferenceListener.invalidating()</tt>.
     *   <li>Call <tt>Lifecycle.deactivate()</tt>, if the instance
     *       implements the <tt>Lifecycle</tt> interface.</li>
     *   <li>Unregister services, if any.</li>
     *   <li>Unbind dependencies, if any.</li>
     *   <li>Dispose instance.</li>
     * </ol>
     * <p>
     * Note: Care must be taken during this callback, because the
     * underlying object associated with the instance reference may
     * not be fully functioning. For example, this event might be
     * fired in direct response to a dependent service shutting down,
     * which then instigates the invalidation of the underlying object
     * instance.
     * @param event the associated instance reference event.
    **/
    public void invalidating(InstanceReferenceEvent event);
}
