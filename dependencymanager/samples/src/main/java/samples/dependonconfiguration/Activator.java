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
            )
        );
    }
    
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
