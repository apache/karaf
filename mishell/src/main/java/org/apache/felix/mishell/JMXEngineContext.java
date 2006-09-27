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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * This class is in charge of most of the interesting stuff. Basically, it keeps the current engine instance being used
 * and creates the OSGiScriptEngineManager 'decorator' that handles the fact that engines can be installed in 
 * separate bundles and then the mechanism for finding engines used by the javax.script API does not work.
 *
 */
public class JMXEngineContext {
	private ScriptEngine engine;
	private OSGiScriptEngineManager engineManager;
	private String language;

	Logger log=Logger.getLogger(JMXEngineContext.class.getName());
	Level l=Level.INFO;
	public JMXEngineContext(String language) throws EngineNotFoundException{
		this(language, null);
	}
	public JMXEngineContext(String lang, OSGiScriptEngineManager engineManager)throws EngineNotFoundException{
		this.engineManager=engineManager;
		if (log.isLoggable(l)){
		StringBuffer msg=new StringBuffer("Available script engines are:");
		log.log(l, "Available script engines are: \n");
		for (ScriptEngineFactory sef : engineManager.getEngineFactories()) {
			msg.append(sef.getEngineName()+"\n");
		}
		log.log(l, msg.toString());
		}
		JMoodProxyManager manager=new JMoodProxyManager();
		engine=engineManager.getEngineByName(lang);
		this.language=engine.getFactory().getLanguageName();
		String managerName=getVarName("manager");
		engineManager.put(managerName, manager);
		for(String key: engineManager.getBindings().keySet()){
			engine.put(getVarName(key), engineManager.get(key));
			}
		
		if (engine==null) throw new EngineNotFoundException(language);
	}
	/**
	 * This method is used because prior versions of jrbuy binding needed global variables
	 * to be called '$'+name. Last version already do that automatically, so this is not needed anymore
	 * @param name
	 * @return
	 */
	//TODO: remove this method when we update to latest version of jruby-engine
	private String getVarName(String name){
		if (engine.getFactory().getEngineName().equals("jruby")) name="$"+name;
		return name;
		
	}
	public ScriptEngine getEngine() {
		return engine;
	}
	public void setEngine(ScriptEngine engine) {
		this.engine = engine;
	}
	public OSGiScriptEngineManager getEngineManager() {
		return engineManager;
	}
	public void setEngineManager(OSGiScriptEngineManager engineManager) {
		this.engineManager = engineManager;
	}
	/**
	 * This methods changes the engine language. It reloads the ScriptEngineManagers to ensure
	 * that any new engines (for example new bundles or updated ones) are discovered.  
	 * @param name
	 * @throws EngineNotFoundException
	 */
	public void setLanguage(String name) throws EngineNotFoundException{
		OSGiScriptEngineManager manager=(OSGiScriptEngineManager)engineManager;
		manager.reloadManagers();
		ScriptEngine newEngine=manager.getEngineByName(name);
		if(newEngine!=null) {
			engine=newEngine;
			language=engine.getFactory().getLanguageName();
			for(String key: engineManager.getBindings().keySet()){
				System.out.println(key);
				engine.put(getVarName(key), engineManager.get(key));
				}

		}
		else throw new EngineNotFoundException("name");
	}
	public String getLanguage(){
		return language;
	}
}	
	
	


