/**
 * 
 */
package org.apache.karaf.shell.web;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * @author Achim
 *
 */
public class WebEventHandler implements EventHandler {
	
	private final Map<Long, String> bundleEvents = new HashMap<Long, String>();

	/* (non-Javadoc)
	 * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
	 */
	public void handleEvent(Event event) {
		String topic = event.getTopic();
		Long bundleID = (Long) event.getProperty("bundle.id");
		getBundleEvents().put(bundleID, topic);
	}

	/**
	 * @return the bundleEvents
	 */
	public Map<Long, String> getBundleEvents() {
		return bundleEvents;
	}

}
