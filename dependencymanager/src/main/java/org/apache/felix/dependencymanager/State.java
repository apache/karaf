package org.apache.felix.dependencymanager;

import java.util.List;

/**
 * Encapsulates the current state of the dependencies of a service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class State {
	public static final String[] STATES = { "?", "inactive", "waiting for required", "tracking optional" };
	public static final int INACTIVE = 1;
	public static final int WAITING_FOR_REQUIRED = 2;
	public static final int TRACKING_OPTIONAL = 3;
	private final List m_deps;
	private final int m_state;
	
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
	
	public int getState() {
		return m_state;
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
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("State[" + STATES[m_state] + "|");
		List deps = m_deps;
    	for (int i = 0; i < deps.size(); i++) {
    		Dependency dep = (Dependency) deps.get(i);
    		buf.append("(" + dep + (dep.isRequired() ? " R" : " O") + (dep.isAvailable() ? " +" : " -") + ")");
    	}
    	return buf.toString();
	}
}
