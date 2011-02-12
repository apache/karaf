package org.apache.karaf.shell.web;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;

public abstract class WebManagerCommand extends OsgiCommandSupport {

	WebEventHandler eventHandler;
	private WarManager warManager;

	
    @Argument(index = 0, name = "ids", description = "The list of bundle IDs separated by whitespaces", required = true, multiValued = true)
    List<Long> ids;

	@Override
	protected Object doExecute() throws Exception {
		if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
            	if (eventHandler.getBundleEvents().containsKey(id)) {
            		WebEvent webEvent = eventHandler.getBundleEvents().get(id);
            		Bundle bundle = webEvent.getBundle();
            		if (bundle == null) {
	                    System.err.println("Bundle ID" + id + " is invalid");
	                } 
            	} else {
            		ids.remove(id);
            	}
            }
        }
        doExecute(ids);
        return null;
	}

	
	/**
	 * @param eventHandler the eventHandler to set
	 */
	public void setEventHandler(WebEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	/**
	 * @return the eventHandler
	 */
	public WebEventHandler getEventHandler() {
		return eventHandler;
	}

	abstract void doExecute(List<Long> bundles) throws Exception;

	/**
	 * @param warManager the warManager to set
	 */
	public void setWarManager(WarManager warManager) {
		this.warManager = warManager;
	}

	/**
	 * @return the warManager
	 */
	public WarManager getWarManager() {
		return warManager;
	}
}