/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.osgi.framework.util;

import java.util.EventListener;
import java.util.EventObject;

/**
 * This interface is used by <tt>DispatchQueue</tt> to dispatch events.
 * Generally speaking, each type of event to dispatch will have an instance
 * of a <tt>Dispatcher</tt> so that the dispatch queue can dispatch to
 * the appropriate listener method for the specific listener type,
 * for example:
 * <pre>
 *  Dispatcher d = new Dispatcher() {
 *      public void dispatch(EventListener l, EventObject eventObj)
 *      {
 *          ((FooListener) l).fooXXX((FooEvent) eventObj);
 *      }
 *  };
 *  FooEvent event = new FooEvent(this);
 *  dispatchQueue.dispatch(d, FooListener.class, event);
 * </pre>
 * In the above code substitute a specific listener and event for the
 * <tt>Foo</tt> listener and event. <tt>Dispatcher</tt>s can be reused, so
 * it is a good idea to cache them to avoid unnecessary memory allocations.
**/
public interface Dispatcher
{
    /**
     * Dispatch an event to a specified event listener.
     *
     * @param l the event listener to receive the event.
     * @param eventObj the event to dispatch.
    **/
    public void dispatch(EventListener l, EventObject eventObj);
}
