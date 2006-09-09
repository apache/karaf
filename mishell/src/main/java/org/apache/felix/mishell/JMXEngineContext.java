package org.apache.felix.mishell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.felix.jmxintrospector.MBeanProxyManager;

//import org.apache.felix.jmood.core.CoreControllerMBean;
//import org.apache.felix.jmood.core.FrameworkMBean;
//import org.apache.felix.jmood.utils.ObjectNames;

public class JMXEngineContext {
	private ScriptEngine engine;
	private ScriptEngineManager engineManager;


	Logger log=Logger.getLogger(JMXEngineContext.class.getName());
	Level l=Level.INFO;

	public JMXEngineContext(String language)throws EngineNotFoundException{
		engineManager=new ScriptEngineManager(this.getClass().getClassLoader());
		log.log(l, "Available script engines are:");
		for (ScriptEngineFactory sef : engineManager.getEngineFactories()) {
			log.log(l, sef.getEngineName());
		}
		engine=engineManager.getEngineByName(language);
		if (engine==null) throw new EngineNotFoundException(language);
		JMoodProxyManager manager=new JMoodProxyManager();
		String managerName=getVarName("manager");
		engine.put(managerName, manager);
	}
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
	public ScriptEngineManager getEngineManager() {
		return engineManager;
	}
	public void setEngineManager(ScriptEngineManager engineManager) {
		this.engineManager = engineManager;
	}
	}
	
	
	


