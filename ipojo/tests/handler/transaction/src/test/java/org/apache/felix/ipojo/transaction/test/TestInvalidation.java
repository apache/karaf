package org.apache.felix.ipojo.transaction.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.io.File;
import java.io.InputStream;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.test.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.helpers.OSGiHelper;
import org.apache.felix.ipojo.tinybundles.BundleAsiPOJO;
import org.apache.felix.ipojo.transaction.test.component.FooDelegator;
import org.apache.felix.ipojo.transaction.test.component.FooImpl;
import org.apache.felix.ipojo.transaction.test.service.CheckService;
import org.apache.felix.ipojo.transaction.test.service.Foo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@RunWith( JUnit4TestRunner.class )
public class TestInvalidation {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private IPOJOHelper ipojo;

    public static final File ROOT = new File("target/tmp");
    public static final File TEST = new File("src/test/resources");


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
        ROOT.mkdirs();


        InputStream service = TinyBundles.newBundle()
            .add(CheckService.class)
            .add(Foo.class)
            .set(Constants.BUNDLE_SYMBOLICNAME,"Service")
            .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
            .build();

        InputStream fooimpl = TinyBundles.newBundle()
            .add(FooImpl.class)
            .set(Constants.BUNDLE_SYMBOLICNAME,"Foo Provider")
            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
            .build( BundleAsiPOJO.asiPOJOBundle(new File(ROOT, "FooImpl.jar"), new File(TEST, "foo.xml"))  );

        InputStream test = TinyBundles.newBundle()
            .add(FooDelegator.class)
            .set(Constants.BUNDLE_SYMBOLICNAME,"RequiredTransactionPropagation")
            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service, javax.transaction")
            .build( BundleAsiPOJO.asiPOJOBundle(new File(ROOT, "requires.jar"), new File(TEST, "requires.xml"))  );


        Option[] opt =  options(
                provision(
                        mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.handler.transaction").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.transaction").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.test.helpers").version(asInProject())
                ),
                provision(
                        service,
                        fooimpl,
                        test
                    )
                ,
                new Customizer() {
                    @Override
                    public InputStream customizeTestProbe( InputStream testProbe )
                    {
                       return TinyBundles.modifyBundle(testProbe)
                           .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service")
                           .build();
                    }
                });
        return opt;
    }




    @Test
    public void testInvalidation() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        final ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requires-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        CheckService cs = (CheckService) osgi.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgi.getServiceObject(TransactionManager.class.getName(), null);

        Thread thread = new Thread (new Runnable() {
           public void run() {
               try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
               prov.dispose();
           }
        });

        thread.start();

        tm.begin();
        Transaction t = tm.getTransaction();
        cs.doSomethingLong(); // 5s, so prov should be disposed during this time and under becomes invalid

        Assert.assertEquals(ComponentInstance.INVALID, under.getState());

        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, t.getStatus());

        t.rollback();
    }


}
