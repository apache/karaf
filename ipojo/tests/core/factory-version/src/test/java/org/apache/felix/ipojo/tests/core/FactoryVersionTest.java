package org.apache.felix.ipojo.tests.core;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOBuilder.withiPOJO;

import java.io.File;

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.tests.core.component.MyComponent;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith( JUnit4TestRunner.class )
public class FactoryVersionTest {

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
                        // mavenBundle().groupId( "org.ops4j.pax.swissbox" ).artifactId( "pax-swissbox-tinybundles" ).version(asInProject())
                        ),
                provision(
                        newBundle()
                            .add( MyService.class )
                           .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterface")
                           .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .build( withBnd() )
                    ),
               provision(
                       // Component V1
                        newBundle()
                            .add(MyComponent.class)
                            .set(Constants.BUNDLE_SYMBOLICNAME,"ProviderV1")
                            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .build( withiPOJO(new File(tmp, "provider-v1.jar"), new File("provider-v1.xml"))),
                     // Component V1.1 (Bundle Version)
                        newBundle()
                            .add(MyComponent.class)
                            .set(Constants.BUNDLE_SYMBOLICNAME,"ProviderV1.1")
                            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .set(Constants.BUNDLE_VERSION, "1.1")
                            .build( withiPOJO(new File(tmp, "provider-v1.1.jar"), new File("provider-v1.1.xml"))),
                // Instance declaration
                newBundle()
                    .set(Constants.BUNDLE_SYMBOLICNAME,"Instances")
                    .build( withiPOJO(new File(tmp, "instances.jar"), new File("instances.xml")))
                    )
                );
        return opt;
    }

    @Test
    public void testDeploy() {
        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }
    }

    @Test
    public void testInstanceArchitecture() {
          // Version 1.0
          ServiceReference refv1 = ipojo.getServiceReferenceByName(Architecture.class.getName(), "instance-v1");
          Assert.assertNotNull(refv1);
          Architecture archv1 = (Architecture) osgi.getServiceObject(refv1);

          String version = archv1.getInstanceDescription().getComponentDescription().getVersion();
          Assert.assertEquals("1.0", version);

          // Version 1.1
          ServiceReference refv11 = ipojo.getServiceReferenceByName(Architecture.class.getName(), "instance-v1.1");
          Assert.assertNotNull(refv11);
          Architecture archv11 = (Architecture) osgi.getServiceObject(refv11);

          String version11 = archv11.getInstanceDescription().getComponentDescription().getVersion();
          Assert.assertEquals("1.1", version11);

          // No Version
          ServiceReference refany = ipojo.getServiceReferenceByName(Architecture.class.getName(), "instance-any");
          Assert.assertNotNull(refany);
          Architecture archany = (Architecture) osgi.getServiceObject(refany);

          String any = archany.getInstanceDescription().getComponentDescription().getVersion();
          Assert.assertNotNull(any);

          // No version set in the factory, so no version.
          ServiceReference refmci = ipojo.getServiceReferenceByName(Architecture.class.getName(), "MyComponentInstance");
          Assert.assertNotNull(refmci);
          Architecture archmcy = (Architecture) osgi.getServiceObject(refmci);

          String mci = archmcy.getInstanceDescription().getComponentDescription().getVersion();
          Assert.assertNull(mci);

    }

    @Test
    public void testServiceProperty() throws InvalidSyntaxException {

          // Version 1.0
          //ServiceReference refv1 = ipojo.getServiceReferenceByName(MyService.class.getName(), "instance-v1");
          ServiceReference[] refv1 = context.getAllServiceReferences(MyService.class.getName(), "(instance.name=instance-v1)");
          Assert.assertNotNull(refv1);
          String version = (String) refv1[0].getProperty("factory.version");
          Assert.assertEquals("1.0", version);

          // Version 1.1
          ServiceReference[] refv11 = context.getAllServiceReferences(MyService.class.getName(), "(instance.name=instance-v1.1)");
          //ServiceReference refv11 = ipojo.getServiceReferenceByName(MyService.class.getName(), "instance-v1.1");
          Assert.assertNotNull(refv11);
          String version11 = (String) refv11[0].getProperty("factory.version");

          Assert.assertEquals("1.1", version11);

          // No Version
          ServiceReference[] refany = context.getAllServiceReferences(MyService.class.getName(), "(instance.name=instance-any)");

          // ServiceReference refany = ipojo.getServiceReferenceByName(MyService.class.getName(), "instance-any");
          Assert.assertNotNull(refany);
          String any = (String) refany[0].getProperty("factory.version");
          Assert.assertNotNull(any);

          // No version set in the factory, so no version.
          ServiceReference[] refmci = context.getAllServiceReferences(MyService.class.getName(), "(instance.name=MyComponentInstance)");
          //ServiceReference refmci = ipojo.getServiceReferenceByName(MyService.class.getName(), "MyComponentInstance");
          Assert.assertNotNull(refmci);
          String mci = (String) refmci[0].getProperty("factory.version");
          Assert.assertNull(mci);
    }


}
