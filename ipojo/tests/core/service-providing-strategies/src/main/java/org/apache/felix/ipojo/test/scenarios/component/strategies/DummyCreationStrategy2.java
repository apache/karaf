package org.apache.felix.ipojo.test.scenarios.component.strategies;

import org.apache.felix.ipojo.IPOJOServiceFactory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.handlers.providedservice.strategy.ConfigurableCreationStrategy;

public class DummyCreationStrategy2 extends ConfigurableCreationStrategy {

	protected IPOJOServiceFactory getServiceFactory(InstanceManager manager) {
		return new DummyServiceFactory(manager);
	}
    
    
}

