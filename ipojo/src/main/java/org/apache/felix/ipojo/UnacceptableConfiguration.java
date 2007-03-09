package org.apache.felix.ipojo;

/**
 * UnacceptableConfiguration is throwed when a factory refuses to create an instance.  
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class UnacceptableConfiguration extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2998931848886223965L;
	
	/**
	 * @param message : message indicating the error.
	 */
	public UnacceptableConfiguration(String message) { super(message); }

}
