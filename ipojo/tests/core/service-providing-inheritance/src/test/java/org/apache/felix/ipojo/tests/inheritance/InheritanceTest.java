package org.apache.felix.ipojo.tests.inheritance;

import static org.apache.felix.ipojo.tinybundles.BundleAsiPOJO.asiPOJOBundle;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.asURL;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.with;

import java.io.File;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.tests.inheritance.a.IA;
import org.apache.felix.ipojo.tests.inheritance.b.IB;
import org.apache.felix.ipojo.tests.inheritance.c.C;
import org.apache.felix.ipojo.tests.inheritance.d.D;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@RunWith( JUnit4TestRunner.class )
public class InheritanceTest {

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
                provision(
                        // Runtime.
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId( "org.ops4j.pax.swissbox" ).artifactId( "pax-swissbox-tinybundles" ).version(asInProject())
                        ),
                // Bundle A
                provision(
                        newBundle()
                            .addClass( IA.class )
                            .prepare()
                           .set(Constants.BUNDLE_SYMBOLICNAME,"A")
                           .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.inheritance.a")
                            .build( asURL() ).toExternalForm()
                    ),
                // Bundle B
                provision(
                        newBundle()
                            .addClass( IB.class )
                            .prepare()
                           .set(Constants.BUNDLE_SYMBOLICNAME,"B")
                           .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.inheritance.a")
                           .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.inheritance.b")
                            .build( asURL() ).toExternalForm()
                    ),
               // Bundle C and D : iPOJO Bundles
               provision(
                       // Component C
                        newBundle()
                            .addClass(C.class)
                            .prepare(
                                    with()
                                        .set(Constants.BUNDLE_SYMBOLICNAME,"C")
                                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.inheritance.b," +
                                                "org.apache.felix.ipojo.tests.inheritance.a")
                                    )
                            .build( asiPOJOBundle(new File(tmp, "provider.jar"), new File("src/test/resources/provider.xml"))).toExternalForm(),
                     // Component D
                        newBundle()
                            .addClass(D.class)
                            .prepare(
                                    with()
                                        .set(Constants.BUNDLE_SYMBOLICNAME,"D")
                                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.inheritance.b," +
                                                "org.apache.felix.ipojo.tests.inheritance.a")
                                    )
                            .build( asiPOJOBundle(new File(tmp, "cons.jar"), new File("src/test/resources/cons.xml"))).toExternalForm())
                );
        return opt;
    }

    @Test
    public void testDeploy() {
        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }

        osgi.waitForService(Architecture.class.getName(), "(architecture.instance=c)", 2000);
        osgi.waitForService(Architecture.class.getName(), "(architecture.instance=d)", 2000);

        Object[] arch = osgi.getServiceObjects(Architecture.class.getName(), null);
        for (Object o : arch) {
            Architecture a = (Architecture) o;
            if ( a.getInstanceDescription().getState() != ComponentInstance.VALID) {
                Assert.fail("Instance " + a.getInstanceDescription().getName() + " not valid : " + a.getInstanceDescription().getDescription());
            }
        }
    }
    
    @Test
    public void testArchitecture() {
        osgi.waitForService(Architecture.class.getName(), "(architecture.instance=d)", 2000);
        ServiceReference ref = ipojo.getServiceReferenceByName(Architecture.class.getName(), "d");
        Assert.assertNotNull(ref);
        
        Architecture arch = (Architecture) osgi.getServiceObject(ref);
        
        System.out.println(arch.getInstanceDescription().getDescription());
        
        Assert.assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());
        DependencyDescription dd = getDependency(arch, "org.apache.felix.ipojo.tests.inheritance.b.IB");
        
        Assert.assertTrue(! dd.getServiceReferences().isEmpty());
        
        ServiceReference dref = (ServiceReference) dd.getServiceReferences().get(0);
        Assert.assertEquals(dref.getBundle().getSymbolicName(), "C");
        
    }
    
    private DependencyDescription getDependency(Architecture arch, String id) {
        DependencyHandlerDescription hd = (DependencyHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:requires");
        Assert.assertNotNull(hd);
        for(DependencyDescription dd : hd.getDependencies()) {
            if (dd.getId().equals(id)) { return dd; }
        }
        Assert.fail("Dependency " + id + " not found");
        return null;
    }

}
