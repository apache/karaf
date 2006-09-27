package org.apache.felix.mishell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class OSGiScriptEngineFinder {
	private BundleContext context;

	public OSGiScriptEngineFinder(BundleContext context) {
		this.context = context;
	}
	private List<String> getAllEngineFactoryCandidates() throws IOException{
		Bundle[] bundles = context.getBundles();
		List<String> factoryCandidates = new ArrayList<String>();
		for (Bundle bundle : bundles) {
			System.out.println(bundle.getSymbolicName());
			if(bundle.getSymbolicName().equals("system.bundle")) continue;
			Enumeration urls = bundle.findEntries("META-INF/services",
					"javax.script.ScriptEngineFactory", false);
			if (urls == null)
				continue;
			while (urls.hasMoreElements()) {
				URL u = (URL) urls.nextElement();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(u.openStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					factoryCandidates.add(line.trim());
					//Just for testing:
					
					try {
						Class engineFactory=Class.forName(line.trim());
						ScriptEngineManager manager=new ScriptEngineManager(engineFactory.getClassLoader());
						for (ScriptEngineFactory f: manager.getEngineFactories()){
							ClassLoader old=Thread.currentThread().getContextClassLoader();
							Thread.currentThread().setContextClassLoader(engineFactory.getClassLoader());
							ScriptEngine e=f.getScriptEngine();//.eval(f.getOutputStatement("Hello world"));
							Thread.currentThread().setContextClassLoader(old);
							e.eval("File.new(\"\")");
							Thread.currentThread().sleep(1000);
						}
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
 catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}
		return factoryCandidates;
	}
	public ScriptEngineManager getManagerFor(String factoryName) throws ClassNotFoundException{
		Class factory=Class.forName(factoryName); 
		String a;
		ScriptEngineManager manager=new ScriptEngineManager(factory.getClassLoader());
		//Does this manager know the factory?
		boolean flag=false;
		for (ScriptEngineFactory fac : manager.getEngineFactories()){
			if(fac.getClass().equals(factory)) flag=true;
		}
		return manager;
		
	}
	/**
	 * Adds Engine factories found in installed bundles. The resolution order is the following:
	 * <ol>
	 * <li>First, all the factories found by the manager are used. This includes all factories seen by 
	 * the class loader that instantiated the ScriptEngineManager
	 * </li>
	 * <li>Then, by order of installation, the rest of the bundles are seeked. Therefore, if an engine appears in
	 * two bundles, only the first one is used. 
	 * </li>
	 * </ol>
	 * This should not be a problem, as this method is intended for sparse use (only when changing the language)
	 * @param the manager to whom the factories are going to be added
	 * @exception RuntimeException wrapper for IOException in case there is any problem with the bundle.findEntries methods
	 * @exception RuntimeException  wrapper for ClassNotFoundException and InstantiationException which are supposed not to happen
	 */
	public void addEngineFactories(ScriptEngineManager manager){
		//TODO refactor all this to make it work. An option is to directly use 
		System.out.println("calling add engine factories");
		try{
		List<String> factoryCandidates=this.getAllEngineFactoryCandidates();
		for (String candidate: factoryCandidates){
			System.out.println("Candidate: "+candidate);
			Class factoryClazz=this.getClass().getClassLoader().loadClass(candidate);
			ScriptEngineFactory factory=(ScriptEngineFactory) factoryClazz.newInstance();
			ScriptEngine engine=factory.getScriptEngine();
			try {
				engine.eval("puts 'Hello world'");
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(manager.getEngineByName(factory.getLanguageName())!=null){
				System.out.println("Engine already existing. Continuing");
				continue; //This means that there already is a factory for that language
			}
			//Now we register the factory for all its names, extensions and mime types
			for(String name: factory.getNames()){
				System.out.println("registering "+name);
				manager.registerEngineName(name, factory);
			}
			for (String type: factory.getMimeTypes()){
				manager.registerEngineMimeType(type, factory);
				System.out.println(manager.getEngineByMimeType(type).equals(factory));
			}
			for (String extension: factory.getExtensions()){
				manager.registerEngineExtension(extension, factory);
				System.out.println(manager.getEngineByExtension(extension).equals(factory));

			}
			}
		System.out.println("manager now has: ");
		System.out.println(manager.getEngineByName("jruby"));
		
		for (ScriptEngineFactory f : manager.getEngineFactories()){
			System.out.println(f);
		}
		}catch (IOException ioe){
			throw new RuntimeException(ioe); 
		} catch (ClassNotFoundException cnfe) {
		} catch (InstantiationException iee) {
			throw new RuntimeException (iee);
		} catch (IllegalAccessException iae) {
			throw new RuntimeException(iae); 
		}
	}
}
	
