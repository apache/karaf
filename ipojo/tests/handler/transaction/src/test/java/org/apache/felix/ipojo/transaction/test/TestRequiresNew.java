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
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOBuilder.withiPOJO;

@RunWith( JUnit4TestRunner.class )
public class TestRequiresNew {

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
        .build( withiPOJO(new File(ROOT, "FooImpl.jar"), new File(TEST, "foo.xml"))  );

        InputStream test = TinyBundles.newBundle()
        .add(FooDelegator.class)
        .set(Constants.BUNDLE_SYMBOLICNAME,"RequiresNewTransactionPropagation")
        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.transaction.test.service, javax.transaction")
        .build( withiPOJO(new File(ROOT, "requiresnew.jar"), new File(TEST, "requiresnew.xml"))  );

        Option[] opt =  options(
                provision(
                        mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.handler.transaction").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.transaction").version(asInProject()),
                        mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject()
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
    public void testOkOutsideTransaction() {
        ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requiresnew-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgi.getServiceObject(ref)).doSomethingGood(); // Ok
    }

    @Test
    public void testOkInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requiresnew-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        CheckService cs = (CheckService) osgi.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgi.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        cs.doSomethingGood();
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertNotSame(t2, t); // Two different transactions
        // Check that t2 is commited => no transaction
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, t2.getStatus());
        t.commit();
    }

    @Test
    public void testExceptionOutsideTransaction() {
        ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requiresnew-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        try {
            ((CheckService) osgi.getServiceObject(ref)).doSomethingBad(); // throws an exception on rollback
            Assert.fail("Exception expected on rollback");
        } catch (IllegalStateException e) {
            // Ok.
        } catch (Exception  e) {
            Assert.fail("IllegalStateException expected on rollback");
        }
    }

    @Test
    public void testExceptionInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requiresnew-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        CheckService cs = (CheckService) osgi.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgi.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad();
            Assert.fail("IllegalStateException expected");  // throws an exception on rollback
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertNotSame(t2, t);
        // Check that t2 is rolledback
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, t2.getStatus());
        Assert.assertEquals(Status.STATUS_ACTIVE, t.getStatus());

        t.commit();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testExpectedExceptionOutsideTransaction() {
        ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requiresnew-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgi.getServiceObject(ref)).doSomethingBad2();
    }

    @Test
    public void testExpectedExceptionInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojo.createComponentInstance("org.apache.felix.ipojo.transaction.test.component.FooImpl");
        ComponentInstance under = ipojo.createComponentInstance("requiresnew-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojo.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        CheckService cs = (CheckService) osgi.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgi.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad2();
            Assert.fail("UnsupportedOperationException expected");
        } catch(Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertNotSame(t2, t);
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, t2.getStatus());
        Assert.assertEquals(Status.STATUS_ACTIVE, t.getStatus());

        t.commit();
    }
}
