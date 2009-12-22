package samples.dependonservice;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createService()
            .setImplementation(DataGenerator.class)
            .add(createServiceDependency()
                .setService(Store.class)
                .setRequired(true)
            )
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false)
            )
        );
    }
    
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
