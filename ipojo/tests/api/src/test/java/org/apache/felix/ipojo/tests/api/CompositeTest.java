package org.apache.felix.ipojo.tests.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.api.Dependency;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.api.Service;
import org.apache.felix.ipojo.api.composite.CompositeComponentType;
import org.apache.felix.ipojo.api.composite.ExportedService;
import org.apache.felix.ipojo.api.composite.ImportedService;
import org.apache.felix.ipojo.api.composite.Instance;
import org.apache.felix.ipojo.api.composite.InstantiatedService;
import org.example.service.Foo;
import org.example.service.impl.FooImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


@RunWith( JUnit4TestRunner.class )
public class CompositeTest {

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
        Option[] opt =  options(
                felix(),
                equinox(),
                provision(
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.composite").version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.api").version(asInProject())
                    )
                );
        return opt;
    }

   @Test
   public void createACompositeWithcontainedInstance() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
       // Define the component types
       PrimitiveComponentType prov = createAProvider();
       PrimitiveComponentType cons = createAConsumer();

       CompositeComponentType type = new CompositeComponentType()
           .setBundleContext(context)
           .setComponentTypeName("comp1")
           .addInstance(new Instance(prov.getFactory().getName()))
           .addInstance(new Instance(cons.getFactory().getName()));

       ComponentInstance ci = type.createInstance();

       assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

       // Stop cons
       cons.stop();
       assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));

       // Restart cons
       cons.start();
       assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

   }

   @Test
   public void createACompositeWithAnInstantiatedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
       // Define the component types
       PrimitiveComponentType prov = createAProvider();
       prov.start();
       PrimitiveComponentType cons = createAConsumer();

       ServiceReference[] refs = osgi.getServiceReferences(Factory.class.getName(),
               "(component.providedServiceSpecifications=" + Foo.class.getName() +")");
       assertThat(refs.length, is(not(0)));

       Factory factory = (Factory) osgi.getServiceObject(refs[0]);
       System.out.println(factory.getComponentDescription().getDescription());

       CompositeComponentType type = new CompositeComponentType()
           .setBundleContext(context)
           .setComponentTypeName("comp2")
           .addSubService(new InstantiatedService().setSpecification(Foo.class.getName()))
           .addInstance(new Instance(cons.getFactory().getName()));

       ComponentInstance ci = type.createInstance();

       System.out.println(ci.getInstanceDescription().getDescription());

       assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

       // Stop prov
       prov.stop();
       assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));

       // Restart prov
       prov.start();
       assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

   }

   @Test
   public void createACompositeWithAnOptionalInstantiatedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
       // Define the component types
       PrimitiveComponentType prov = createAProvider();
       prov.start();

       CompositeComponentType type = new CompositeComponentType()
           .setBundleContext(context)
           .setComponentTypeName("comp3")
           .addSubService(new InstantiatedService().setSpecification(Foo.class.getName()).setOptional(true));

       ComponentInstance ci = type.createInstance();

       assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

       // Stop prov
       prov.stop();
       assertThat("ci is valid - 1", ci.getState(), is(ComponentInstance.VALID));

       // Restart prov
       prov.start();
       assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

   }

   @Test
   public void createACompositeWithAnImportedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
       // Define the component types
       PrimitiveComponentType prov = createAProvider();
       prov.createInstance();
       PrimitiveComponentType cons = createAConsumer();

       ServiceReference[] refs = osgi.getServiceReferences(Factory.class.getName(),
               "(component.providedServiceSpecifications=" + Foo.class.getName() +")");
       assertThat(refs.length, is(not(0)));

       CompositeComponentType type = new CompositeComponentType()
           .setBundleContext(context)
           .setComponentTypeName("comp2")
           .addSubService(new ImportedService().setSpecification(Foo.class.getName()))
           .addInstance(new Instance(cons.getFactory().getName()));

       ComponentInstance ci = type.createInstance();

       System.out.println(ci.getInstanceDescription().getDescription());

       assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

       // Stop prov
       prov.stop();
       assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));

       // Restart prov
       prov.start();
       prov.createInstance();
       assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

   }

   @Test
   public void createACompositeWithAnOptionalImportedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
       // Define the component types
       PrimitiveComponentType prov = createAProvider();
       prov.createInstance();

       CompositeComponentType type = new CompositeComponentType()
           .setBundleContext(context)
           .setComponentTypeName("comp3")
           .addSubService(new ImportedService().setSpecification(Foo.class.getName()).setOptional(true));

       ComponentInstance ci = type.createInstance();

       assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

       // Stop prov
       prov.stop();
       assertThat("ci is valid - 1", ci.getState(), is(ComponentInstance.VALID));

       // Restart prov
       prov.start();
       prov.createInstance();
       assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

   }

   @Test
   public void createACompositeWithExportingAService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
       // Define the component types
       PrimitiveComponentType prov = createAProvider();
       prov.start();
       PrimitiveComponentType cons = createAConsumer();
       ComponentInstance c = cons.createInstance();

         CompositeComponentType type = new CompositeComponentType()
           .setBundleContext(context)
           .setComponentTypeName("compExport")
           .addSubService(new InstantiatedService().setSpecification(Foo.class.getName()))
           .addService(new ExportedService().setSpecification(Foo.class.getName()));

       ComponentInstance ci = type.createInstance();

       assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));
       assertThat("c is valid", c.getState(), is(ComponentInstance.VALID));


       // Stop prov
       prov.stop();
       assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));
       assertThat("c is invalid", c.getState(), is(ComponentInstance.INVALID));


       // Restart prov
       prov.start();
       assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));
       assertThat("c is valid - 2", c.getState(), is(ComponentInstance.VALID));


   }

    private PrimitiveComponentType createAProvider() {
        return new PrimitiveComponentType()
        .setBundleContext(context)
        .setClassName(FooImpl.class.getName())
        .setPublic(true)
        .addService(new Service()); // Provide the FooService
    }

    private PrimitiveComponentType createAConsumer() {
        return new PrimitiveComponentType()
        .setBundleContext(context)
        .setClassName(org.example.service.impl.MyComponentImpl.class.getName())
        .addDependency(new Dependency().setField("myFoo"))
        .setValidateMethod("start");
    }



}
