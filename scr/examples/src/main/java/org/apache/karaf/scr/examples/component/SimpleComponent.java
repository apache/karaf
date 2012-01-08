package org.apache.karaf.scr.examples.component;

import org.apache.karaf.scr.examples.service.ExampleService;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component(name = SimpleComponent.COMPONENT_NAME)
public class SimpleComponent {

    public static final String COMPONENT_NAME = "SimpleComponent";

    public static final String COMPONENT_LABEL = "Example Component";

    private LogService logService;

    private ExampleService exampleService;

    /**
     * Called when all of the SCR Components required dependencies have been
     * satisfied
     * 
     */
    @Activate
    public void activate() {
        logService
                .log(LogService.LOG_INFO, "Activating the " + COMPONENT_LABEL);
        exampleService.setName("Scott");
        exampleService.setSalutation("Hello");
        exampleService.printGreetings();
    }

    /**
     * Called when any of the SCR Components required dependencies become
     * unsatisfied
     * 
     */
    @Deactivate
    public void deactivate() {
        logService.log(LogService.LOG_INFO, "Dectivating the "
                + COMPONENT_LABEL);
    }

    @Reference
    public void setExampleService(final ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    public void unsetExampleService(final ExampleService exampleService) {
        this.exampleService = null;
    }

    @Reference
    protected void setLogService(LogService logService) {
        this.logService = logService;
    }

    protected void unsetLogService(LogService logService) {
        this.logService = null;
    }
}
