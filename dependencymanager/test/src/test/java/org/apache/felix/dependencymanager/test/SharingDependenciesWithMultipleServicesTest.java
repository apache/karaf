package org.apache.felix.dependencymanager.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Service;
import org.apache.felix.dependencymanager.dependencies.BundleDependency;
import org.apache.felix.dependencymanager.dependencies.ConfigurationDependency;
import org.apache.felix.dependencymanager.dependencies.ServiceDependency;
import org.apache.felix.dependencymanager.impl.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

@RunWith(JUnit4TestRunner.class)
public class SharingDependenciesWithMultipleServicesTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.2.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.4")
            )
        );
    }    
    
    @Test
    public void testShareServiceDependencyWithMultipleServices(BundleContext context) {
        DependencyManager m = new DependencyManager(context, new Logger(context));
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service provider = m.createService().setImplementation(new ServiceProvider()).setInterface(ServiceInterface.class.getName(), null);
        ServiceDependency dependency = m.createServiceDependency().setService(ServiceInterface.class).setRequired(true);
        Service consumer1 = m.createService().setImplementation(new ServiceConsumer(e, 1)).add(dependency);
        Service consumer2 = m.createService().setImplementation(new ServiceConsumer(e, 4)).add(dependency);
        
        m.add(provider);
        m.add(consumer1);
        e.waitForStep(3, 2000);
        m.add(consumer2);
        e.waitForStep(6, 2000);
        m.remove(provider);
        m.remove(consumer1);
        m.remove(consumer2);
    }
    
    @Test
    public void testShareConfigurationDependencyWithMultipleServices(BundleContext context) {
        DependencyManager m = new DependencyManager(context, new Logger(context));
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Service provider = m.createService().setImplementation(new ConfigurationProvider(e)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        ConfigurationDependency dependency = m.createConfigurationDependency().setPid("test");
        Service consumer1 = m.createService().setImplementation(new ConfigurationConsumer(e, 2)).add(dependency);
        Service consumer2 = m.createService().setImplementation(new ConfigurationConsumer(e, 3)).add(dependency);
        
        // add the configuration provider that should publish the configuration as step 1
        m.add(provider);
        // add the first consumer, and wait until its updated() method is invoked
        m.add(consumer1);
        e.waitForStep(2, 2000);
        // add the second consumer, and wait until its updated() method is invoked
        m.add(consumer2);
        e.waitForStep(3, 2000);
        // break down the test again
        m.remove(provider);
        m.remove(consumer1);
        m.remove(consumer2);
    }
    
    @Test
    public void testShareBundleDependencyWithMultipleServices(BundleContext context) {
        DependencyManager m = new DependencyManager(context, new Logger(context));
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        BundleDependency dependency = m.createBundleDependency().setFilter("(Bundle-SymbolicName=org.apache.felix.dependencymanager)").setRequired(true);
        Service consumer1 = m.createService().setImplementation(new BundleConsumer(e, 1)).add(dependency);
        Service consumer2 = m.createService().setImplementation(new BundleConsumer(e, 2)).add(dependency);
        
        m.add(consumer1);
        e.waitForStep(1, 2000);
        m.add(consumer2);
        e.waitForStep(2, 2000);
        m.remove(consumer2);
        m.remove(consumer1);
    }
    
    static interface ServiceInterface {
        public void invoke(Runnable r);
    }

    static class ServiceProvider implements ServiceInterface {
        public void invoke(Runnable r) {
            r.run();
        }
    }
    
    static class ServiceConsumer implements Runnable {
        private volatile ServiceInterface m_service;
        private final Ensure m_ensure;
        private int m_step;

        public ServiceConsumer(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }

        public void start() {
            Thread t = new Thread(this);
            t.start();
        }

        public void run() {
            m_ensure.step(m_step);
            m_service.invoke(new Runnable() { public void run() { m_ensure.step(m_step + 1); } });
            m_ensure.step(m_step + 2);
        }
    }
    
    static class ConfigurationConsumer implements ManagedService {
        private final Ensure m_ensure;
        private int m_step;

        public ConfigurationConsumer(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }

        public void updated(Dictionary properties) throws ConfigurationException {
            if (properties != null) {
                m_ensure.step(m_step);
            }
        }
    }
    
    static class ConfigurationProvider {
        private final Ensure m_ensure;
        private volatile ConfigurationAdmin m_configAdmin;
        
        public ConfigurationProvider(Ensure ensure) {
            m_ensure = ensure;
        }
        
        public void init() {
            try {
                org.osgi.service.cm.Configuration conf = m_configAdmin.getConfiguration("test", null);
                conf.update(new Properties() {{ put("testkey", "testvalue"); }} );
                m_ensure.step(1);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    static class BundleConsumer {
        private final Ensure m_ensure;
        private int m_step;

        public BundleConsumer(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }
        
        public void start() {
            m_ensure.step(m_step);
        }
    }
}
