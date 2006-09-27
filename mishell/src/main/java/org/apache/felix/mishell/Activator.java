/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.mishell;

import org.apache.felix.mishell.console.Console;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private Console console;

	public void start(BundleContext context) throws Exception {
		//NOTE: new javax.script.EngineManager() uses context class loader
		//We've changed that to use EngineManager(ClassLoader loader)
		//Alternatively, we could instantiate it in a separate thread with proper context class loader set.
		//TODO: not sure wether resources are correctly freed. A running script will probably keep running, for example, if it has got threads
		//but that would hang the console, which is quite primitive yet. (it would need a 'run [script] &' idiom)
		console = new Console(new JMXEngineContext("javascript", new OSGiScriptEngineManager(context)));
		Thread t=new Thread(console);
		//At least JRuby engine (maybe others too) uses context class loaders internally
		//So this is to prevent  problems with that: context cl=console class loader which in turn
		//loads the manager and therefore engine factory
//		t.setContextClassLoader(this.getClass().getClassLoader());
		t.setName("ConsoleThread");
		t.start();

	}

	public void stop(BundleContext context) throws Exception {
		console.stop();

	}

}
