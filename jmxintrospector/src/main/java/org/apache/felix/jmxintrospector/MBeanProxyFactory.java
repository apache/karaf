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

package org.apache.felix.jmxintrospector;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/*import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
*/
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.InterfaceMaker;

import org.objectweb.asm.Type;

/**
 * 
 * The MBeanProxyFactory is the central element of the jmxintrospector library. The main method is the {@link MBeanProxyFactory#newProxyInstance(String)}
 * method, that accepts an objectname in String form and returns a proxy for that mbean.
 * Internally, it uses the cglib library, although it be made to work with other bytecode libraries. More specifically,prior versions used the Javassist library   
 */

public class MBeanProxyFactory {
	private MBeanServerConnection mbeanServer;
	
	public MBeanProxyFactory(){
	}
	
	public MBeanProxyFactory(MBeanServerConnection mbeanServer) {
		super();
		this.mbeanServer = mbeanServer;
	}

	/**
	 * Internal method for generating the Class object for the dynamically-generated interface for the mbean.
	 * The name given to the class is based on the actual classname of the mbean, although some extensions are added
	 * to it by the cglib library to avoid namespace clashes. 
	 * @param oname
	 * @return
	 * @throws IOException 
	 * @throws InstanceNotFoundException 
	 * @throws ClassNotFoundException 
	 * @throws ReflectionException 
	 * @throws IntrospectionException 
	 * @throws  
	 * @throws MalformedObjectNameException 
	 * @throws Exception
	 */
	private Class getInterface(String oname) throws MalformedObjectNameException, InstanceNotFoundException, IOException, IntrospectionException, ReflectionException {
		ObjectName objectName=ObjectName.getInstance(oname);
		String ifaceName=mbeanServer.getObjectInstance(objectName).getClassName();
		//uses the ifaceName as the prefix for the class name
		InterfaceMaker maker=new MBeanInterfaceMaker(ifaceName);
			for (Signature s : getSignatures(objectName)) {
				//add each method
				maker.add(s, null);
			}
				return maker.create();
		}
	private Type getType(String type) throws ClassNotFoundException{
		return JmxAsmHelper.getAsmType(type);
	}
	/**
	 * Internal method for generating the signatures of the mbeans. 
	 * @param objectName
	 * @return
	 * @throws IOException 
	 * @throws ReflectionException 
	 * @throws IntrospectionException 
	 * @throws InstanceNotFoundException 
	 */
	private List<Signature> getSignatures(ObjectName objectName)throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException{
		List<Signature> methods=new ArrayList<Signature>();
		MBeanInfo minfo;
		MBeanAttributeInfo[] attributes=null;
			minfo = mbeanServer.getMBeanInfo(objectName);
			attributes=minfo.getAttributes();
		
		for (MBeanAttributeInfo info : attributes) {
			String name=info.getName().substring(0, 1).toUpperCase()+info.getName().substring(1);
			if(info.isReadable()){
				//For each readable attribute, we generate a getter method (following the isXX for booleans
				//when it is being used on the remote side)
			if(info.isIs()){
				methods.add(new Signature("is"+name, Type.BOOLEAN_TYPE, new Type[0]));
			}
			else{
				try{
				methods.add(new Signature("get"+name, getType(info.getType()), new Type[0]));
				}catch(ClassNotFoundException cnfe){
					System.out.println("JMXINTROSPECTOR WARNING: "+info.getType()+" could not be found. Attribute will not be added to proxy.");
					continue;
				}	
				}
			}
			//Same with each writable att, but with setters.
			if(info.isWritable()){
				try{
				Type [] params=new Type[]{getType(info.getType())};
				Signature s=new Signature("set"+name, Type.VOID_TYPE, params);
				methods.add(s);
				}catch(ClassNotFoundException cnfe){
					System.out.println("JMXINTROSPECTOR WARNING: "+info.getType()+" could not be found. Attribute will not be added to proxy.");
					continue;
				}	
			}
		}
		//same for each operation
		for (MBeanOperationInfo info : minfo.getOperations()) {
			try{
			Type[] params=new Type[info.getSignature().length];
			for (int i = 0; i < params.length; i++) {
				params[i]=getType(info.getSignature()[i].getType());
			}
			
			Signature s=new Signature(info.getName(), getType(info.getReturnType()), params);
			methods.add(s);
			}catch(ClassNotFoundException cnfe){
				System.out.println("JMXINTROSPECTOR WARNING: "+info.toString()+" could not be created. Operation will not be added to proxy.");
				continue;
				
			}

		}
		return methods;
	}
	/**
	 * Returns a proxy object for the MBean specified by the object name oname in the
	 *  mbean server associated with this factory. This proxy object implements the generated interface plus
	 *  the MBean interface. It will also be a notification broadcaster if the underlying mbean broadcasts notifications.
	 * @param oname
	 * @return 
	 * @throws IOException 
	 * @throws ReflectionException 
	 * @throws IntrospectionException 
	 * @throws InstanceNotFoundException 
	 * @throws  
	 * @throws MalformedObjectNameException 
	 * @throws Exception
	 */
	public Object newProxyInstance(String oname) throws MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, IOException{
		ObjectName objectName=ObjectName.getInstance(oname);
		Class iface=getInterface(oname);
		MBeanInfo info=mbeanServer.getMBeanInfo(objectName);
		boolean isBroadcaster=false;
		MBeanNotificationInfo[] notifs=info.getNotifications();
		if (notifs!=null && notifs.length!=0) isBroadcaster=true;
		//We first create the proxy for the remote mbean. If broadcasting supported, then it adds the broadcasting interface
		Object proxy=MBeanServerInvocationHandler.newProxyInstance(mbeanServer, objectName, iface, isBroadcaster);
		//We get the underlying invocation handler, needed for the wrapper handler. The wrapper adds the mbean interface functionality
		//and integrates JMX invocation handler. 
		InvocationHandler h=Proxy.getInvocationHandler(proxy);
		InvocationHandler wrapper=new JMXInvocationHandler(oname,mbeanServer, mbeanServer.getMBeanInfo(objectName), h);
		Class[] ifaces;
		if (isBroadcaster) {
			ifaces=new Class[]{iface, NotificationEmitter.class, MBean.class};
		}else ifaces=new Class[]{iface, MBean.class};
		//finally, we create the proxy with the appropriate classloader, the interfaces and the invocation handler
		Object mbeanProxy=Proxy.newProxyInstance(proxy.getClass().getClassLoader(), ifaces, wrapper);
		return mbeanProxy;
	}
	private class JMXInvocationHandler implements InvocationHandler, MBean{
		private String objectName;
		private MBeanServerConnection mBeanServer;
		private InvocationHandler mbeanHandler;
		private MBeanInfo mbeanInfo;
		
		public JMXInvocationHandler(String objectName, MBeanServerConnection mbeanServer, MBeanInfo mBeanInfo, InvocationHandler mbeanHandler) {
			super();
			this.objectName = objectName;
			this.mBeanServer = mbeanServer;
			this.mbeanHandler = mbeanHandler;
			this.mbeanInfo =mBeanInfo;
			
		}
		
		//FIXME: hashCode() and equals do not work if not exposed in management interface. I have not thought of a workaround for that yet.
		//note that this is needed for hashmaps and comparisons. 
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			//decide wether to invoke the JMXInvocationHandler we have implemented 
			//or use the one that was created by the MBeanServerInvocationHandler
			if(method.getDeclaringClass().equals(MBean.class)){
				return this.getClass().getMethod(method.getName(), null).invoke(this, args);
			}
			else return mbeanHandler.invoke(proxy, method, args);
		}

		public MBeanServerConnection getMBeanServer() {
			return mBeanServer;
		}

		public String getObjectName() {
			return objectName;
		}

		public MBeanInfo getMBeanInfo() {
			return mbeanInfo;
		}


		
	}

	public MBeanServerConnection getMbeanServer() {
		return mbeanServer;
	}

	public void setMbeanServer(MBeanServerConnection mbs) {
		this.mbeanServer = mbs;
	}
	/**
	 * This class is used to be able to modify the super class, 
	 * because setNamePrefix and setAttemptLoad are protected methods
	 * 
	 */
	private class MBeanInterfaceMaker extends InterfaceMaker{
		public MBeanInterfaceMaker(String namePrefix) {
			super();
			super.setNamePrefix(namePrefix);
			super.setAttemptLoad(true);
		}
	}

}
