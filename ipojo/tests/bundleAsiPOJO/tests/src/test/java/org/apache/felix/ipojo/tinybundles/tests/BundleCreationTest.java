package org.apache.felix.ipojo.tinybundles.tests;

import static org.apache.felix.ipojo.tinybundles.BundleAsiPOJO.asiPOJOBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.with;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.felix.ipojo.pax.tinybundles.tests.impl.Consumer;
import org.apache.felix.ipojo.pax.tinybundles.tests.impl.MyProvider;
import org.apache.felix.ipojo.tinybundles.tests.service.Hello;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


@RunWith( JUnit4TestRunner.class )
public class BundleCreationTest {


    @Inject
    BundleContext context;

    @Configuration
    public static Option[] configure()
    {


        File metaProv = new File("provider.xml");
        if (! metaProv.exists()) {
            Assert.fail("No provider file");
        }

        return options(
            felix(),
            equinox(),
            knopflerfish(),
            provision(
                    mavenBundle()
                    .groupId( "org.ops4j.pax.swissbox" )
                    .artifactId( "pax-swissbox-tinybundles" )
                    .version( asInProject() )),
            provision(
                    mavenBundle()
                    .groupId("org.apache.felix")
                    .artifactId("org.apache.felix.ipojo")
                    .version ( asInProject() )
            ),
             provision(newBundle()
                     .add(Hello.class)
                     .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterface")
                     .set(Constants.EXPORT_PACKAGE,"org.apache.felix.ipojo.tinybundles.tests.service")
                     .build(TinyBundles.withBnd())),
            provision(
                    newBundle()
                    .add(MyProvider.class)
                    .set(Constants.BUNDLE_SYMBOLICNAME,"Provider")
                    .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tinybundles.tests.service")
                    .build( asiPOJOBundle(new File("provider.jar"), new File("provider.xml")))),
            provision(
                    newBundle()
                    .add(Consumer.class)
                    .set(Constants.BUNDLE_SYMBOLICNAME, "Consumer")
                    .set(Constants.IMPORT_PACKAGE,
                             "org.apache.felix.ipojo.tinybundles.tests.service")
                    .build(
                           asiPOJOBundle(new File("consumer.xml"))
                    )));
    }

    @Test
    public void creation() throws MalformedURLException, BundleException, InvalidSyntaxException {
        assertBundle("ServiceInterface");
//        createServiceProvider();
        assertBundle("Provider");
        assertBundle("Consumer");

        dumpBundles();
        dumpServices();
        // Check service
        Assert.assertNotNull(context.getAllServiceReferences(Hello.class.getName(), null));

    }


    private void dumpServices() throws InvalidSyntaxException {
        ServiceReference[] refs = context.getAllServiceReferences(null, null);
        System.out.println(" === Services === ");
        for (ServiceReference ref : refs) {
            String[] itf = (String[]) ref.getProperty(Constants.OBJECTCLASS);
            System.out.println(itf[0]);
        }
        System.out.println("====");
    }

    private void dumpBundles() throws InvalidSyntaxException {
        Bundle[] bundles = context.getBundles();
        System.out.println(" === Bundles === ");
        for (Bundle bundle : bundles) {
            String sn  =  bundle.getSymbolicName();
            System.out.println(sn);
        }
        System.out.println("====");
    }

    private void assertBundle(String sn) {
        for (Bundle bundle :context.getBundles()) {
            if (bundle.getSymbolicName().equals(sn)
                    && bundle.getState() == Bundle.ACTIVE) {
                return;
            }

        }
        Assert.fail("Cannot find the bundle " + sn);
    }



}
