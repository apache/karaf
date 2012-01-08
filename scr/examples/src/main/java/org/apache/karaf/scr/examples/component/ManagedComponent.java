package org.apache.karaf.scr.examples.component;

import java.util.Map;

import org.apache.karaf.scr.examples.service.ExampleService;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component( 
        name = ManagedComponent.COMPONENT_NAME,
        configurationPolicy = ConfigurationPolicy.require)
public class ManagedComponent {

    public static final String COMPONENT_NAME = "ManagedComponent";

    public static final String COMPONENT_LABEL = "Example Managed Component";
    
    private static final String COMPONENT_PROP_NAME = "name";
    
    private static final String COMPONENT_PROP_SALUTATION = "salutation";

    private LogService logService;

    private ExampleService exampleService;

    /**
     * Called when all of the SCR Components required dependencies have been
     * satisfied
     * 
     */
    @Activate
    public void activate(final Map<String, ?> properties) {
        logService
                .log(LogService.LOG_INFO, "Activating the " + COMPONENT_LABEL);
        exampleService.setName((String) properties.get(COMPONENT_PROP_NAME));
        exampleService.setSalutation((String) properties.get(COMPONENT_PROP_SALUTATION));
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
        this.logService = logService;
    }
}
