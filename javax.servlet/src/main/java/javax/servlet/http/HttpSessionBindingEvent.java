/*
 * Copyright 1999,2005 The Apache Software Foundation.
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


package javax.servlet.http;

import java.util.EventObject;


/**
 *
 * Sent to an object that implements
 * {@link HttpSessionBindingListener} when the object is
 * bound to or unbound from the session.
 *
 * <p>The session binds the object by a call to
 * <code>HttpSession.putValue</code> and unbinds the object
 * by a call to <code>HttpSession.removeValue</code>.
 *
 *
 *
 * @author		Various
 * @version		$Version$
 *
 * @see 		HttpSession
 * @see 		HttpSessionBindingListener
 *
 */

public class HttpSessionBindingEvent extends EventObject {




    /* The name to which the object is being bound or unbound */

    private String name;



    /**
     *
     * Constructs an event that notifies an object that it
     * has been bound to or unbound from a session.
     * To receive the event, the object must implement
     * {@link HttpSessionBindingListener}.
     *
     *
     *
     * @param session 	the session to which the object is bound or unbound
     *
     * @param name 	the name with which the object is bound or unbound
     *
     * @see			#getName
     * @see			#getSession
     *
     */

    public HttpSessionBindingEvent(HttpSession session, String name) {
	super(session);
	this.name = name;
    }






	/**
	 *
     * Returns the name with which the object is bound to or
	 * unbound from the session.
	 *
	 *
	 * @return		a string specifying the name with which
	 *			the object is bound to or unbound from
	 *			the session
	 *
	 *
	 */
	
	public String getName() {
	return name;
	}






    /**
     *
     * Returns the session to or from which the object is
     * bound or unbound.
     *
     * @return		the session to which the object is
     *			bound or from which the object is
     *			unbound
     *
     *
     *
     */

    public HttpSession getSession() {
	return (HttpSession) getSource();
    }
}







