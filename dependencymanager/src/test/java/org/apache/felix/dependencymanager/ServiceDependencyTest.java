package org.apache.felix.dependencymanager;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.BundleContext;

public class ServiceDependencyTest extends TestCase {
	/**
	 * Checks the basic life-cycle of a service that has no dependencies.
	 * Makes sure that the init, start, stop and destroy callbacks are
	 * invoked on the implementation.
	 */
	public void testStandaloneService() throws Exception {
		// setup the mock objects
	    MockControl c1 = MockControl.createControl(BundleContext.class);
		BundleContext context = (BundleContext) c1.getMock();
        MockControl c2 = MockControl.createControl(MyService.class);
		MyService svc = (MyService) c2.getMock();
		svc.init();
		svc.start();
		svc.stop();
		svc.destroy();
		// start the actual test
		c1.replay();
        c2.replay();
		DependencyManager dm = new DependencyManager(context);
		Service service = new ServiceImpl(context).setImplementation(svc);
		dm.add(service);
		dm.remove(service);
		// verify the results
		c1.verify();
        c2.verify();
	}
	
	/**
	 * Defines a service with one required dependency that is not
	 * available. Makes sure the service is not started.
	 */
	public void testRequiredUnavailableDependency() throws Exception {
		// setup the mock objects
        MockControl c1 = MockControl.createControl(BundleContext.class);
        BundleContext context = (BundleContext) c1.getMock();
        MockControl c2 = MockControl.createControl(MyService.class);
        MyService svc = (MyService) c2.getMock();
        MockControl c3 = MockControl.createControl(Dependency.class);
        Dependency dependency = (Dependency) c3.getMock();

        dependency.isRequired();
        c3.setReturnValue(true, MockControl.ONE_OR_MORE);
        dependency.isAvailable();
        c3.setReturnValue(false, MockControl.ONE_OR_MORE);
        
		dependency.start((Service) null);
        c3.setMatcher(MockControl.ALWAYS_MATCHER);
		dependency.stop((Service) null);
        c3.setMatcher(MockControl.ALWAYS_MATCHER);
		// start the actual test
		c1.replay();
        c2.replay();
        c3.replay();
		DependencyManager dm = new DependencyManager(context);
		Service service = new ServiceImpl(context)
			.setImplementation(svc)
			.add(dependency);
		dm.add(service);
		dm.remove(service);
		// verify the results
		c1.verify();
        c2.verify();
        c3.verify();
	}

	/**
	 * Defines a service with one required dependency that is
	 * available. Makes sure the service is started.
	 */
//	public void testRequiredAvailableDependency() throws Exception {
//		// setup the mock objects
//		IMocksControl ctrl = EasyMock.createControl();
//		BundleContext context = ctrl.createMock(BundleContext.class);
//		Dependency dependency = ctrl.createMock(Dependency.class);
//		MyService svc = ctrl.createMock(MyService.class);
//		ctrl.checkOrder(false);
//		EasyMock.expect(dependency.isRequired()).andReturn(Boolean.TRUE).anyTimes();
//		EasyMock.expect(dependency.isAvailable()).andReturn(Boolean.TRUE).anyTimes();
//		dependency.start((Service) EasyMock.anyObject());
//		svc.init();
//		svc.start();
//		svc.stop();
//		svc.destroy();
//		dependency.stop((Service) EasyMock.anyObject());
//		// start the actual test
//		ctrl.replay();
//		DependencyManager dm = new DependencyManager(context);
//		Service service = new ServiceImpl(context)
//			.setImplementation(svc)
//			.add(dependency);
//		dm.add(service);
//		dm.remove(service);
//		// verify the results
//		ctrl.verify();
//	}
	
	/**
	 * Defines a service with an optional dependency that is not available.
	 * Makes sure the service is started.
	 */
//	public void testOptionalDependency() throws Exception {
//		// setup the mock objects
//		IMocksControl ctrl = EasyMock.createControl();
//		BundleContext context = ctrl.createMock(BundleContext.class);
//		Dependency dependency = ctrl.createMock(Dependency.class);
//		MyService svc = ctrl.createMock(MyService.class);
//		ctrl.checkOrder(false);
//		EasyMock.expect(dependency.isRequired()).andReturn(Boolean.FALSE).anyTimes();
//		EasyMock.expect(dependency.isAvailable()).andReturn(Boolean.FALSE).anyTimes();
//		dependency.start((Service) EasyMock.anyObject());
//		svc.init();
//		svc.start();
//		svc.stop();
//		svc.destroy();
//		dependency.stop((Service) EasyMock.anyObject());
//		// start the actual test
//		ctrl.replay();
//		DependencyManager dm = new DependencyManager(context);
//		Service service = new ServiceImpl(context)
//			.setImplementation(svc)
//			.add(dependency);
//		dm.add(service);
//		dm.remove(service);
//		// verify the results
//		ctrl.verify();
//	}
	
	public void XtestRequiredAvailableServiceDependency() throws Exception {
//		// setup the mock objects
//		IMocksControl ctrl = EasyMock.createControl();
//		BundleContext context = ctrl.createMock(BundleContext.class);
//		Filter filter = ctrl.createMock(Filter.class);
//		MyService svc = ctrl.createMock(MyService.class);
//		ctrl.checkOrder(false);
//		EasyMock.expect(context.createFilter("(objectClass=org.apache.felix.dependencymanager.DummyService)")).andReturn(filter);
//		context.addServiceListener((ServiceListener) EasyMock.anyObject());
//		EasyMock.expect(context.getServiceReferences(null, "EasyMock for interface org.osgi.framework.Filter")).andReturn(new ServiceReference[] {});
//		context.removeServiceListener((ServiceListener) EasyMock.anyObject());
//		svc.init();
//		svc.start();
//		svc.stop();
//		svc.destroy();
//		// start the actual test
//		ctrl.replay();
//		DependencyManager dm = new DependencyManager(context);
//		Service service = new ServiceImpl(context)
//			.setImplementation(svc)
//			.add(new ServiceDependency(context)
//				.setRequired(true)
//				.setService(DummyService.class));
//		dm.add(service);
//		dm.remove(service);
//		// verify the results
//		ctrl.verify();
	}
}

interface MyService {
	public void init();
	public void start();
	public void stop();
	public void destroy();
}

interface DummyService {
}
