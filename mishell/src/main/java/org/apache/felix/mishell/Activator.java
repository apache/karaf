package org.apache.felix.mishell;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private Console console;

	public void start(BundleContext context) throws Exception {
		
		//NOTE: new javax.script.EngineManager() uses context class loader
		//We've changed that to use EngineManager(ClassLoader loader)
		//Alternatively, we could instantiate it in a separate thread with proper context class loader set.
		console = new Console("javascript", null);
		Thread t=new Thread(console);
		//At least JRuby engine (maybe others too) uses context class loaders internally
		//So this is to prevent  problems with that: context cl=console class loader which in turn
		//loads the manager and therefore engine factory
		t.setContextClassLoader(this.getClass().getClassLoader());
		t.setName("ConsoleThread");
		t.start();

	}

	public void stop(BundleContext context) throws Exception {
		console.stop();

	}

}
