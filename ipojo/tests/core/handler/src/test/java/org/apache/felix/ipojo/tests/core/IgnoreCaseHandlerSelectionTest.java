package org.apache.felix.ipojo.tests.core;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOBuilder.withiPOJO;


import java.io.File;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.tests.core.component.MyComponent;
import org.apache.felix.ipojo.tests.core.handler.EmptyHandler;
import org.apache.felix.ipojo.tests.core.service.MyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

/**
 * Check that the handler selection ignore case.
 * An empty handler declared with
 * name="EmPtY" and namespace="orG.apAche.feliX.iPOJO.tests.CORE.hAnDlEr"
 * is declared, and two instances using this handler are created. The test is
 * successful is the two instances are created correctly.
 * Test about Felix-1318 : Case mismatch problem of iPOJO custom handler name
 */
@RunWith( JUnit4TestRunner.class )
public class IgnoreCaseHandlerSelectionTest {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private IPOJOHelper ipojo;

    @Before
    public void init() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
    }

    @After
    public void stop() {
        ipojo.dispose();
        osgi.dispose();
    }

    @Configuration
    public static Option[] configure() {

        File tmp = new File("target/tmp");
        tmp.mkdirs();


        Option[] opt =  options(
                felix(),
                equinox(),
                knopflerfish(),
                provision(
                        // Runtime.
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject()
                        ),
                provision(
                        newBundle()
                            .add( MyService.class )
                            .set( Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface" )
                            .set( Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service" )
                            .build( TinyBundles.withBnd() )
                    ),
               provision(
                       // Components and the handler
                        newBundle()
                            .add(MyComponent.class) // Component Implementation
                            .add(EmptyHandler.class) // Handler.
                            .set(Constants.BUNDLE_SYMBOLICNAME,"IgnoreCase")
                            .set(Constants.IMPORT_PACKAGE,
                                    "org.apache.felix.ipojo.tests.core.service, " +
                                    "org.apache.felix.ipojo, " +
                                    "org.apache.felix.ipojo.metadata")
                            .build(withiPOJO(new File(tmp, "ignorecase.jar"), new File("src/test/resources/ignorecase.xml")))));
        return opt;
    }

    @Test
    public void testDeploy() {
        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }
    }

    /**
     * Checks that the handler is declared and accessible.
     */
    @Test
    public void testHandlerAvailability() {
        ServiceReference[] refs = osgi.getServiceReferences(HandlerFactory.class.getName(), null);
        for (ServiceReference ref : refs) {
            String name = (String) ref.getProperty("handler.name");
            String ns = (String) ref.getProperty("handler.namespace");
            if (name != null
                    && name.equalsIgnoreCase("EmPtY") // Check with ignore case.
                    && ns != null
                    && ns.equalsIgnoreCase("orG.apAche.feliX.iPOJO.tests.CORE.hAnDlEr")) { // Check with ignore case.
                Integer state = (Integer) ref.getProperty("factory.state");
                if (state != null) {
                    Assert.assertEquals(Factory.VALID, state.intValue());
                    return; // Handler found and valid.
                } else {
                    Assert.fail("Handler found but no state exposed");
                }
            }
        }
        Assert.fail("Handler not found");
    }

    /**
     * Check that the instance is correctly created with "empty".
     */
    @Test
    public void testCreationOfIgnoreCase1() {
          ServiceReference refv1 = ipojo.getServiceReferenceByName(Architecture.class.getName(), "IgnoreCase-1");
          Assert.assertNotNull(refv1);
          Architecture arch = (Architecture) osgi.getServiceObject(refv1);
          Assert.assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());

          HandlerDescription desc = arch.getInstanceDescription()
              .getHandlerDescription("orG.apAche.feliX.iPOJO.tests.CORE.hAnDlEr:EmPtY");  // Check with the declared name.

          Assert.assertNotNull(desc);
          Assert.assertTrue(desc.isValid());
    }

    /**
     * Check that the instance is correctly created with "eMptY".
     */
    @Test
    public void testCreationOfIgnoreCase2() {
          ServiceReference refv1 = ipojo.getServiceReferenceByName(Architecture.class.getName(), "IgnoreCase-2");
          Assert.assertNotNull(refv1);
          Architecture arch = (Architecture) osgi.getServiceObject(refv1);
          Assert.assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());

          HandlerDescription desc = arch.getInstanceDescription()
              .getHandlerDescription("org.apache.felix.ipojo.tests.core.handler:empty"); // Check with different case.
          Assert.assertNotNull(desc);
          Assert.assertTrue(desc.isValid());
    }


}
