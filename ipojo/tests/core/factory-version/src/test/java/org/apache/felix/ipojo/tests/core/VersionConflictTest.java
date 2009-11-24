package org.apache.felix.ipojo.tests.core;

import static org.apache.felix.ipojo.tinybundles.BundleAsiPOJO.asiPOJOBundle;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.asFile;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.with;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.test.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.helpers.OSGiHelper;
import org.apache.felix.ipojo.tests.core.component.MyComponent;
import org.apache.felix.ipojo.tests.core.component.MyCons;
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
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

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
    public static Option[] configure() throws MalformedURLException {

        File tmp = new File("target/tmp");
        tmp.mkdirs();

        String url1 =  // Version 1
            newBundle()
            .addClass( MyService.class )
            .prepare()
           .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterfaceV1")
           .set(Constants.BUNDLE_VERSION, "1.0.0")
           .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"1.0.0\"")
            .build( asFile(new File(tmp, "ServiceInterfaceV1.jar"))).toURL().toExternalForm();

        String url2 = // Version 2
                newBundle()
                    .addClass( MyService.class )
                    .prepare()
                   .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterfaceV2")
                   .set(Constants.BUNDLE_VERSION, "2.0.0")
                   .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"2.0.0\"")
            .build( asFile(new File(tmp, "ServiceInterfaceV2.jar"))).toURL().toExternalForm();


        String c1 = newBundle()
            .addClass(MyComponent.class)
            .prepare(
              with()
                  .set(Constants.BUNDLE_SYMBOLICNAME,"ProviderV1")
                  .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[1.0.0, 1.0.0]\"")
              )
              .build( asiPOJOBundle(new File(tmp, "vprovider-v1.jar"), new File("vprovider-v1.xml"))).toExternalForm();

      String c2 = newBundle()
          .addClass(MyComponent.class)
          .prepare(
              with()
                  .set(Constants.BUNDLE_SYMBOLICNAME,"ProviderV2")
                  .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[2.0.0, 2.0.0]\"")
                  .set(Constants.BUNDLE_VERSION, "2.0")
              )
              .build( asiPOJOBundle(new File(tmp, "vprovider-v2.0.jar"), new File("vprovider-v2.xml"))).toExternalForm();

      String cons =   newBundle()
        .addClass(MyCons.class)
        .prepare(
            with()
                .set(Constants.BUNDLE_SYMBOLICNAME,"MyCons")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[2.0.0, 2.0.0]\"")
                .set(Constants.BUNDLE_VERSION, "2.0")
            )
        .build( asiPOJOBundle(new File(tmp, "cons.jar"), new File("cons.xml"))).toExternalForm();


      String consV1 =   newBundle()
      .addClass(MyCons.class)
      .prepare(
          with()
              .set(Constants.BUNDLE_SYMBOLICNAME,"MyCons")
              .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service; version=\"[1.0.0, 1.0.0]\"")
              .set(Constants.BUNDLE_VERSION, "1.0")
          )
      .build( asiPOJOBundle(new File(tmp, "consv1.jar"), new File("cons.xml"))).toExternalForm();

        Option[] opt =  options(
                felix(),
                equinox(),
                knopflerfish(),
                provision(
                        // Runtime.
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.test.helpers").version(asInProject()),
                        mavenBundle().groupId( "org.ops4j.pax.swissbox" ).artifactId( "pax-swissbox-tinybundles" ).version(asInProject())
                        ),
                        systemProperty( "url1" ).value( url1 ),
                        systemProperty( "url2" ).value( url2 ),

                        systemProperty( "c1" ).value( c1 ),
                        systemProperty( "c2" ).value( c2 ),
                        systemProperty( "cons" ).value( cons ),
                        systemProperty( "consV1" ).value( consV1 )



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
