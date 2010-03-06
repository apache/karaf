package samples.dependonconfiguration;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createService()
            .setImplementation(Task.class)
            .add(createConfigurationDependency()
                .setPid("config.pid")
                .setHeading("English Dictionary") 
                .setDescription("Configuration for the EnglishDictionary Service")
                .add(createPropertyMetaData()
                    .setCardinality(Integer.MAX_VALUE)
                    .setType(String.class)
                    .setHeading("English Words")
                    .setDescription("Declare here some valid english words")
                    .setDefaults(new String[] {"hello", "world"})
                    .setId("words")))
        );
    }
    
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
