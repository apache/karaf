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
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.InterfaceMaker;

import org.objectweb.asm.Type;

public class MBeanProxyFactory {
	private MBeanServerConnection mbeanServer;
	
	public MBeanProxyFactory(){
	}
	
	public MBeanProxyFactory(MBeanServerConnection mbeanServer) {
		super();
		this.mbeanServer = mbeanServer;
	}

	private Class getInterface(String oname)throws Exception{
		ObjectName objectName=ObjectName.getInstance(oname);
		String ifaceName=mbeanServer.getObjectInstance(objectName).getClassName();
		InterfaceMaker maker=new MBeanInterfaceMaker(ifaceName);
			for (Signature s : getSignatures(objectName)) {
				maker.add(s, null);
			}
			try {
				return maker.create();
			} catch (Exception e) {
				System.out.println(e.getCause());
				throw e;
			}
		}
	private Type getType(String type) throws Exception{
		System.out.println("getting Type for: "+type);
		return JmxAsmHelper.getAsmType(type);
	}
	private List<Signature> getSignatures(ObjectName objectName)throws Exception{
		List<Signature> methods=new ArrayList<Signature>();
		MBeanInfo minfo =mbeanServer.getMBeanInfo(objectName);
		
		for (MBeanAttributeInfo info : minfo.getAttributes()) {
			String name=info.getName().substring(0, 1).toUpperCase()+info.getName().substring(1);
			if(info.isReadable()){
			if(info.isIs()){
				methods.add(new Signature("is"+name, Type.BOOLEAN_TYPE, new Type[0]));
			}
			else{
//				try {
				
				methods.add(new Signature("get"+name, getType(info.getType()), new Type[0]));
//				}catch (NotFoundException nfe) {
//					String n=nfe.getMessage();
//					String notFoundName=n.startsWith("[")?n.substring(1, n.length()):n;
//					Class notFound=(Class)mbeanServer.invoke(ObjectName.getInstance(TypeGetterMBean.MBEAN_NAME), "getType", new Object[] {notFoundName}, new String[] {String.class.getName()});
//					
//					m=new CtMethod(pool.get(info.getType()), "get"+ name,null, ctIface);
//				}
				}
			}
			if(info.isWritable()){
				Type [] params=new Type[]{getType(info.getType())};
				Signature s=new Signature("set"+name, Type.VOID_TYPE, params);
				methods.add(s);
			}
		}
		for (MBeanOperationInfo info : minfo.getOperations()) {
			Type[] params=new Type[info.getSignature().length];
			for (int i = 0; i < params.length; i++) {
				params[i]=getType(info.getSignature()[i].getType());
			}
			Signature s=new Signature(info.getName(), getType(info.getReturnType()), params);
			methods.add(s);

		}
		return methods;
	}
	/**
	 * Returns a proxy object for the MBean specified by the object name oname in the
	 *  mbean server associated with this factory. This proxy object implements
	 * @param oname
	 * @return
	 * @throws Exception
	 */
	public Object newProxyInstance(String oname)throws Exception{
		ObjectName objectName=ObjectName.getInstance(oname);
		Class iface=getInterface(oname);
		MBeanInfo info=mbeanServer.getMBeanInfo(objectName);
		boolean isBroadcaster=false;
		MBeanNotificationInfo[] notifs=info.getNotifications();
		if (notifs!=null && notifs.length!=0) isBroadcaster=true;
		Object proxy=MBeanServerInvocationHandler.newProxyInstance(mbeanServer, objectName, iface, isBroadcaster);
		InvocationHandler h=Proxy.getInvocationHandler(proxy);
		InvocationHandler wrapper=new JMXInvocationHandler(oname,mbeanServer, mbeanServer.getMBeanInfo(objectName), h);
		Class[] ifaces;
		if (isBroadcaster) {
			ifaces=new Class[]{iface, NotificationEmitter.class, MBean.class};
		}else ifaces=new Class[]{iface, MBean.class};
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
		
		//FIXME: hashCode() and equals do not work if not exposed in management interface
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
	private class MBeanInterfaceMaker extends InterfaceMaker{
		public MBeanInterfaceMaker(String namePrefix) {
			super();
			super.setNamePrefix(namePrefix);
			super.setAttemptLoad(true);
		}
	}

}
