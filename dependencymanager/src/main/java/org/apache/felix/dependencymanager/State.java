/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dependencymanager;

import java.util.List;

/**
 * Encapsulates the current state of the dependencies of a service. A state is
 * basically an immutable value object.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class State {
    private static final String[] STATES = { "?", "inactive", "waiting for required", "tracking optional" };
    private static final int INACTIVE = 1;
    private static final int WAITING_FOR_REQUIRED = 2;
    private static final int TRACKING_OPTIONAL = 3;
	private final List m_deps;
	private final int m_state;
    private String m_stringValue;
	
	/**
	 * Creates a new state instance.
	 * 
	 * @param deps the dependencies that determine the state
	 * @param isActive <code>true</code> if the service is active (started)
	 */
	public State(List deps, boolean isActive) {
		m_deps = deps;
		// only bother calculating dependencies if we're active
    	if (isActive) {
    		boolean allRequiredAvailable = true;
    		for (int i = 0; i < deps.size(); i++) {
    			Dependency dep = (Dependency) deps.get(i);
    			if (dep.isRequired()) {
    				if (!dep.isAvailable()) {
    					allRequiredAvailable = false;
    				}
    			}
    		}
	    	if (allRequiredAvailable) {
	    		m_state = TRACKING_OPTIONAL;
	    	}
	    	else {
	    		m_state = WAITING_FOR_REQUIRED;
	    	}
    	}
    	else {
    		m_state = INACTIVE;
    	}
	}
	
	public boolean isInactive() {
		return m_state == INACTIVE;
	}
	
	public boolean isWaitingForRequired() {
		return m_state == WAITING_FOR_REQUIRED;
	}
	
	public boolean isTrackingOptional() {
		return m_state == TRACKING_OPTIONAL;
	}
	
	public List getDependencies() {
		return m_deps;
	}
	
	public synchronized String toString() {
	    if (m_stringValue == null) {
	        // we only need to determine this once, but we do it lazily
	        StringBuffer buf = new StringBuffer();
    		buf.append("State[" + STATES[m_state] + "|");
    		List deps = m_deps;
        	for (int i = 0; i < deps.size(); i++) {
        		Dependency dep = (Dependency) deps.get(i);
        		buf.append("(" + dep + (dep.isRequired() ? " R" : " O") + (dep.isAvailable() ? " +" : " -") + ")");
        	}
        	m_stringValue = buf.toString();
	    }
        return m_stringValue;
	}
}
