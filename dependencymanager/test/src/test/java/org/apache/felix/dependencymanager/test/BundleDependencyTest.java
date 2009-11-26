package org.apache.felix.dependencymanager.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import junit.framework.Assert;

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Service;
import org.apache.felix.dependencymanager.impl.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class BundleDependencyTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.2.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    
    
    @Test
    public void testBundleDependencies(BundleContext context) {
        DependencyManager m = new DependencyManager(context, new Logger(context));
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Consumer c = new Consumer(e);
        Service consumer = m.createService().setImplementation(c).add(m.createBundleDependency().setCallbacks("add", "remove"));
//        Service consumerWithFilter = m.createService().setImplementation(new Consumer(e)).add(m.createBundleDependency().setFilter(""));
//        Service consumerWithStateMask = m.createService().setImplementation(new Consumer(e)).add(m.createBundleDependency().setStateMask(0));
        // add the service consumer
        m.add(consumer);
        // check if at least one bundle was found
        c.check();
        // remove the consumer again
        m.remove(consumer);
        // check if all bundles were removed correctly
        c.doubleCheck();
    }
    
    static class Consumer {
        private final Ensure m_ensure;
        private int m_count = 0;

        public Consumer(Ensure e) {
            m_ensure = e;
        }
        
        public void add(Bundle b) {
            Assert.assertNotNull("bundle instance must not be null", b);
            m_count++;
        }
        
        public void check() {
            Assert.assertTrue("we should have found at least one bundle", m_count > 0);
        }
        
        public void remove(Bundle b) {
            m_count--;
        }
        
        public void doubleCheck() {
            Assert.assertTrue("all bundles we found should have been removed again", m_count == 0);
        }
    }
}
