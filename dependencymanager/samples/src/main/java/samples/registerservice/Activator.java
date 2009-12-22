package samples.registerservice;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createService()
            .setInterface(Store.class.getName(), null)
            .setImplementation(MemoryStore.class)
        );
    }
    
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
