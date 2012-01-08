package org.apache.karaf.scr.examples.component;

import java.util.Map;

import org.apache.karaf.scr.examples.service.ExampleService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

@Component( 
        name = MetaTypeManagedComponent.COMPONENT_NAME,
        designateFactory = MetaTypeManagedComponentConfig.class)
public class MetaTypeManagedComponent {

    public static final String COMPONENT_NAME = "MetaTypeManagedComponent";

    public static final String COMPONENT_LABEL = "Example Managed Component with MetaType";

    private LogService logService;

    private ExampleService exampleService;

    /**
     * Called when all of the SCR Components required dependencies have been
     * satisfied
     * 
     */
    @Activate
    public void activate(final Map<String, ?> properties, ComponentContext componentContext) {
        logService
                .log(LogService.LOG_INFO, "Activating the " + COMPONENT_LABEL);
        
        MetaTypeManagedComponentConfig config = Configurable.createConfigurable(
        		MetaTypeManagedComponentConfig.class, properties);
        
        exampleService.setName(config.name());
        exampleService.setSalutation(config.salutation());
        for (int i = 0; i < config.numberOfGreetings(); i++) {
            exampleService.printGreetings();
		}
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
