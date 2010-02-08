package org.apache.felix.ipojo.tests.core;

import static org.apache.felix.ipojo.tinybundles.BundleAsiPOJO.asiPOJOBundle;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.test.helpers.OSGiHelper;
import org.apache.felix.ipojo.tests.core.component.DummyImpl;
import org.apache.felix.ipojo.tests.core.handler.DummyHandler;
import org.apache.felix.ipojo.tests.core.service.Dummy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.User;

import aQute.lib.osgi.Constants;

@RunWith(JUnit4TestRunner.class)
public class DummyHandlerTest {

    private static final String DUMMY_TEST_FACTORY = "dummy.test";

    /*
     * Number of mock object by test.
     */
    private static final int NB_MOCK = 10;

  
    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
    }

    @After
    public void tearDown() {
        osgi.dispose();
    }

    @Configuration
    public static Option[] configure() {
        Option[] platform = options(CoreOptions.felix());

        Option[] bundles = 
            options(
                    provision(
                         newBundle()
                             .add(DummyHandler.class) 
                             .build(asiPOJOBundle(new File("src/test/resources/dummy-handler.xml")))
                         ),
                     provision(
                         newBundle()
                             .add(Dummy.class)
                             .add(DummyImpl.class)
                             .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.handler.dummy.test")
                             .build(asiPOJOBundle(new File("src/test/resources/dummy-component.xml")))
                         ),
                    provision(
                        mavenBundle().groupId("org.apache.felix").artifactId("org.osgi.compendium").versionAsInProject()
                            ));
        Option[] r = OptionUtils.combine(platform, bundles);

        return r;
    }

    /**
     * iPOJO Bunles
     * @return
     */
    @Configuration
    public static Option[] configAdminBundle() {
        return options(
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject(), 
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.test.helpers").versionAsInProject());
    }

    /**
     * Mockito bundle
     * @return
     */
    @Configuration
    public static Option[] mockitoBundle() {
        return options(JUnitOptions.mockitoBundles());
    }

    /**
     * Basic Test, in order to know if the instance is correctly create.
     */
    @Test
    public void testDummyTestInstance() {
        ComponentInstance instance = null;

        // Get the factory
        Factory factory = Tools.getValidFactory(osgi, DUMMY_TEST_FACTORY);
        Assert.assertNotNull(factory);

        // Create an instance
        try {
            instance = factory.createComponentInstance(null);
        } catch (UnacceptableConfiguration e) {
            new AssertionError(e);
        } catch (MissingHandlerException e) {
            new AssertionError(e);
            e.printStackTrace();
        } catch (ConfigurationException e) {
            new AssertionError(e);
        }

        // Must be valid now
        Assert.assertEquals(instance.getState(), ComponentInstance.VALID);

        // Stop the instance
        instance.stop();
        Assert.assertEquals(instance.getState(), ComponentInstance.STOPPED);

        // Start the instance
        instance.start();
        Assert.assertEquals(instance.getState(), ComponentInstance.VALID);
    }

    /**
     * Test if the bind and unbind methods are called when the bind service are registered after the instance creation
     */
    @Test
    public void testDummyTestBindAfterStart() {
        ComponentInstance instance = null;

        // Get the factory
        Factory factory = Tools.getValidFactory(osgi, DUMMY_TEST_FACTORY);

        // Create an instance
        try {
            instance = factory.createComponentInstance(null);
        } catch (UnacceptableConfiguration e) {
        } catch (MissingHandlerException e) {
        } catch (ConfigurationException e) {
        }

        Map<User, ServiceRegistration> registrations = new HashMap<User, ServiceRegistration>();

        for (int i = 0; i < NB_MOCK; i++) {
            User service = mock(User.class);
            ServiceRegistration sr = context.registerService(User.class.getName(), service, null);
            registrations.put(service, sr);
        }
        
        //verify that the bind method of the handler has been called
        for (User user : registrations.keySet()) {
                verify(user).getName();
        }
        
        //verify that the unbind has been called
        for (User user : registrations.keySet()) {
            registrations.get(user).unregister();
            verify(user).getType();
        }
        
        //verify no more interaction
        for (User user : registrations.keySet()) {
                Mockito.verifyNoMoreInteractions(user);
        }
    }
    

    /**
     * Test if the bind and unbind methods when the bind services are registered before the instance creation
     */
    @Test
    public void testDummyTestBindBeforeStart() {
        ComponentInstance instance = null;
        
        Map<User, ServiceRegistration> registrations = new HashMap<User, ServiceRegistration>();

        for (int i = 0; i < NB_MOCK; i++) {
            User service = mock(User.class);
            ServiceRegistration sr = context.registerService(User.class.getName(), service, null);
            registrations.put(service, sr);
        }

        // Get the factory
        Factory factory = Tools.getValidFactory(osgi, DUMMY_TEST_FACTORY);

        // Create an instance
        try {
            instance = factory.createComponentInstance(null);
        } catch (UnacceptableConfiguration e) {
        } catch (MissingHandlerException e) {
        } catch (ConfigurationException e) {
        }
        
        //verify that the bind method of the handler has been called
        for (User user : registrations.keySet()) {
                verify(user).getName();
        }
        
        //verify that the unbind has been called
        for (User user : registrations.keySet()) {
            registrations.get(user).unregister();
            verify(user).getType();
        }
        
        //verify no more interaction
        for (User user : registrations.keySet()) {
                Mockito.verifyNoMoreInteractions(user);
        }
    }
}
