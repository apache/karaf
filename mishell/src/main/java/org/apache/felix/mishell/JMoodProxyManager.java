package org.apache.felix.mishell;

import java.util.List;

import javax.management.MBeanServerConnection;

import org.apache.felix.jmxintrospector.MBean;
import org.apache.felix.jmxintrospector.MBeanProxyManager;

public class JMoodProxyManager extends MBeanProxyManager {
	public List<Object> getControllers(){
		return findAll("type=controller");
	}
	public List<Object> getBundlesAt(Object controller){
		MBeanServerConnection server=((MBean)controller).getMBeanServer();
		return findAll("type=bundle", server);
	}
	public List<Object> getServicesAt(Object controller){
		MBeanServerConnection server=((MBean)controller).getMBeanServer();
		return findAll("type=service", server);
	}
	public List<Object> getPackagesAt(Object controller){
		MBeanServerConnection server=((MBean)controller).getMBeanServer();
		return findAll("type=package", server);
	}
	
}
