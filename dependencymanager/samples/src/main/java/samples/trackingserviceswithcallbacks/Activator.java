package samples.trackingserviceswithcallbacks;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createService()
            .setImplementation(DocumentTranslator.class)
            .add(createServiceDependency()
                .setService(Translator.class)
                .setRequired(false)
                .setCallbacks("added", "removed")
            )
        );
    }
    
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
