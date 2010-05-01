package org.apache.felix.ipojo.tests.core;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOBuilder.withiPOJO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.tests.core.component.MyComponent;
import org.apache.felix.ipojo.tests.core.component.MyCons;
import org.apache.felix.ipojo.tests.core.service.MyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.io.StreamUtils;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith( JUnit4TestRunner.class )
public class VersionConflictTest {

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
    public static Option[] configure() throws NullArgumentException, FileNotFoundException, IOException {

        File tmp = new File("target/tmp");
        tmp.mkdirs();

        File f1 = new File(tmp, "service-interface-v1.jar");
        StreamUtils.copyStream(
                newBundle()
                .add( MyService.class )
               .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterfaceV1")
               .set(Constants.BUNDLE_VERSION, "1.0.0")
               .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"1.0.0\"")
               .build( withBnd()),
                new FileOutputStream(f1),
                true);

        File f2 = new File(tmp, "service-interface-v2.jar");
        StreamUtils.copyStream(
                newBundle()
                .add( MyService.class )
                .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterfaceV2")
                .set(Constants.BUNDLE_VERSION, "2.0.0")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"2.0.0\"")
                .build( withBnd()),
                new FileOutputStream(f2),
                true);

        File c1 = new File(tmp, "component-v1.jar");
        StreamUtils.copyStream(
                newBundle()
               .add(MyComponent.class)
               .set(Constants.BUNDLE_SYMBOLICNAME,"ProviderV1")
               .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[1.0.0, 1.0.0]\"")
               .build( withiPOJO(new File("vprovider-v1.xml"))),
               new FileOutputStream(c1),
               true);

        File c2 = new File(tmp, "component-v2.jar");
        StreamUtils.copyStream(
                newBundle()
               .add(MyComponent.class)
               .set(Constants.BUNDLE_SYMBOLICNAME,"ProviderV2")
               .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[2.0.0, 2.0.0]\"")
               .build( withiPOJO(new File("vprovider-v2.xml"))),
               new FileOutputStream(c2),
               true);

        File cons = new File(tmp, "cons.jar");
        StreamUtils.copyStream(
                newBundle()
               .add(MyCons.class)
               .set(Constants.BUNDLE_SYMBOLICNAME,"MyCons")
               .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[2.0.0, 2.0.0]\"")
               .set(Constants.BUNDLE_VERSION, "2.0")
               .build(withiPOJO(new File("cons.xml"))),
               new FileOutputStream(cons),
               true);

        File consV1 = new File(tmp, "cons-v1.jar");
        StreamUtils.copyStream(
                newBundle()
               .add(MyCons.class)
               .set(Constants.BUNDLE_SYMBOLICNAME,"MyCons")
               .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[1.0.0, 1.0.0]\"")
               .set(Constants.BUNDLE_VERSION, "1.0")
               .build(withiPOJO(new File("cons.xml"))),
               new FileOutputStream(consV1),
               true);

        Option[] opt =  options(
                felix(),
                equinox(),
                knopflerfish(),
                provision(
                        // Runtime.
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject(),                        mavenBundle().groupId("org.ops4j.base").artifactId("ops4j-base-lang").versionAsInProject()
//                        mavenBundle().groupId( "org.ops4j.pax.swissbox" ).artifactId( "pax-swissbox-tinybundles" ).version(asInProject())
                        ),
                        systemProperty( "url1" ).value( f1.toURI().toURL().toExternalForm() ),
                        systemProperty( "url2" ).value( f2.toURI().toURL().toExternalForm() ),

                        systemProperty( "c1" ).value( c1.toURI().toURL().toExternalForm() ),
                        systemProperty( "c2" ).value( c2.toURI().toURL().toExternalForm() ),
                        systemProperty( "cons" ).value( cons.toURI().toURL().toExternalForm() ),
                        systemProperty( "consV1" ).value( consV1.toURI().toURL().toExternalForm() )
                );
        return opt;
    }

    @Test
    public void deployBundlesAtRuntime() throws MalformedURLException, BundleException, InvalidSyntaxException {

       Bundle b1 = context.installBundle(context.getProperty("url1"));
       b1.start();


       Bundle b3 = context.installBundle(context.getProperty("c1"));
       b3.start();

        Bundle b2 = context.installBundle(context.getProperty("url2"));
        b2.start();

        Bundle b4 = context.installBundle(context.getProperty("c2"));
        b4.start();

        Bundle b5 = context.installBundle(context.getProperty("cons"));
        b5.start();


        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            System.out.println("bundle " + bundles[i].getSymbolicName() + " : " + (bundles[i].getState() == Bundle.ACTIVE));
            //Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }


        PackageAdmin pa = osgi.getPackageAdmin();
        Bundle b = pa.getBundles("ServiceInterfaceV1", null)[0];
        ExportedPackage[] packages = pa.getExportedPackages(b);
        if (packages == null) {
            System.out.println("Packages  ServiceInterfaceV1 : " + 0);
        } else {
            System.out.println("Packages  ServiceInterfaceV1 : " + packages.length);
            for (ExportedPackage p : packages) {
                System.out.println("Package : " + p.getName() + " - " + p.getVersion().toString());
            }
        }
        b = pa.getBundles("ServiceInterfaceV2", null)[0];
        packages = pa.getExportedPackages(b);
        System.out.println("Packages  ServiceInterfaceV2 : " + packages.length);
        for (ExportedPackage p : packages) {
            System.out.println("Package : " + p.getName() + " - " + p.getVersion().toString());
        }

        osgi.waitForService(Architecture.class.getName(), "(architecture.instance=mycons)", 2000);

        // Check that the two services are provided.
        ServiceReference[] refs = context.getAllServiceReferences(MyService.class.getName(), null);
        Assert.assertNotNull(refs);
        Assert.assertEquals(2, refs.length);

        ServiceReference refv1 = ipojo.getServiceReferenceByName(Architecture.class.getName(), "mycons");
        Assert.assertNotNull(refv1);
        Architecture arch = (Architecture) osgi.getServiceObject(refv1);

        HandlerDescription desc = arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:requires");
        Assert.assertNotNull(desc);

        DependencyHandlerDescription d = (DependencyHandlerDescription) desc;
        Assert.assertNotNull(d.getDependencies());
        Assert.assertEquals(1, d.getDependencies().length);

        DependencyDescription dep = d.getDependencies()[0];
        Assert.assertEquals(Dependency.RESOLVED, dep.getState());

        Assert.assertEquals(1, dep.getServiceReferences().size());
        ServiceReference r = (ServiceReference) dep.getServiceReferences().get(0);
        Assert.assertEquals("provider", r.getProperty("factory.name"));
        Assert.assertEquals("2.0", r.getProperty("factory.version"));
    }

    @Test
    public void deployBundlesAtRuntimeV1() throws MalformedURLException, BundleException, InvalidSyntaxException {

       Bundle b1 = context.installBundle(context.getProperty("url1"));
       b1.start();


       Bundle b3 = context.installBundle(context.getProperty("c1"));
       b3.start();

        Bundle b2 = context.installBundle(context.getProperty("url2"));
        b2.start();

        Bundle b4 = context.installBundle(context.getProperty("c2"));
        b4.start();

        Bundle b5 = context.installBundle(context.getProperty("consV1"));
        b5.start();


        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            System.out.println("bundle " + bundles[i].getSymbolicName() + " : " + (bundles[i].getState() == Bundle.ACTIVE));
            //Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }


        PackageAdmin pa = osgi.getPackageAdmin();
        Bundle b = pa.getBundles("ServiceInterfaceV1", null)[0];
        ExportedPackage[] packages = pa.getExportedPackages(b);
        if (packages == null) {
            System.out.println("Packages  ServiceInterfaceV1 : " + 0);
        } else {
            System.out.println("Packages  ServiceInterfaceV1 : " + packages.length);
            for (ExportedPackage p : packages) {
                System.out.println("Package : " + p.getName() + " - " + p.getVersion().toString());
            }
        }
        b = pa.getBundles("ServiceInterfaceV2", null)[0];
        packages = pa.getExportedPackages(b);
        System.out.println("Packages  ServiceInterfaceV2 : " + packages.length);
        for (ExportedPackage p : packages) {
            System.out.println("Package : " + p.getName() + " - " + p.getVersion().toString());
        }

        osgi.waitForService(Architecture.class.getName(), "(architecture.instance=mycons)", 2000);

        // Check that the two services are provided.
        ServiceReference[] refs = context.getAllServiceReferences(MyService.class.getName(), null);
        Assert.assertNotNull(refs);
        Assert.assertEquals(2, refs.length);

        ServiceReference refv1 = ipojo.getServiceReferenceByName(Architecture.class.getName(), "mycons");
        Assert.assertNotNull(refv1);
        Architecture arch = (Architecture) osgi.getServiceObject(refv1);

        HandlerDescription desc = arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:requires");
        Assert.assertNotNull(desc);

        DependencyHandlerDescription d = (DependencyHandlerDescription) desc;
        Assert.assertNotNull(d.getDependencies());
        Assert.assertEquals(1, d.getDependencies().length);

        DependencyDescription dep = d.getDependencies()[0];
        Assert.assertEquals(Dependency.RESOLVED, dep.getState());

        Assert.assertEquals(1, dep.getServiceReferences().size());
        ServiceReference r = (ServiceReference) dep.getServiceReferences().get(0);

        Assert.assertEquals("provider", r.getProperty("factory.name"));
        Assert.assertEquals("1.0", r.getProperty("factory.version"));
    }






}
