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

package org.apache.felix.jmood.core;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.apache.felix.jmood.core.ManagedBundleMBean;
import org.apache.felix.jmood.core.ManagedServiceMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import org.apache.felix.jmood.utils.ObjectNames;


public class BundleMBeanTestCase extends TestHarness {
//	private String bname="es.upm.dit.jmood;0.9.0";
//	private static Logger l=Logger.getLogger(BundleMBeanTestCase.class.getPackage().getName());
//	private ObjectName jmood;
	public BundleMBeanTestCase() throws Exception{
		super();
//		jmood=new ObjectName(ObjectNames.BUNDLE+bname);
	}
	protected void setUp()throws Exception{
		super.setUp();
	}
	protected void tearDown()throws Exception{
		super.tearDown();
	}
	public void testAttributes() throws Exception{
//		super.testAttributes(jmood,ManagedBundleMBean.class);
//		l.finest("testing Start level attribute");
//		getServer().getAttribute(jmood, "StartLevel");
	}
	public void testUpdateInvariant()throws Exception{
		//JMood update should be invariant
		//TODO Should we refresh?
//		getServer().invoke(jmood,"update",null, null);
//		boolean wait=true;
//		String state=null;
//		for(int i=0;wait&&i<10;i++){
//			try{
//				Thread.sleep(10);
//		state=(String)(getServer().getAttribute(jmood,"State"));
//		wait=state.equals("STOPPING")||state.equals("STARTING");
//			}catch (InstanceNotFoundException e){}
//		}
//		assertTrue("state: "+state,state.equals("ACTIVE"));
	}
	public void testServiceMBeans()throws Exception{
//		Iterator it=getServer().queryNames(new ObjectName(ObjectNames.ALLSERVICES), null).iterator();
//		while(it.hasNext()){
//			super.testAttributes((ObjectName) it.next(),ManagedServiceMBean.class);
//		}
	}
	public void testBundleMBeans()throws Exception{
//		Iterator it=getServer().queryNames(new ObjectName(ObjectNames.ALLBUNDLES), null).iterator();
//		while(it.hasNext()){
//			super.testAttributes((ObjectName) it.next(),ManagedBundleMBean.class);
//		}
	}
	public void testPackageMBeans()throws Exception{
//		//Currently fails for Felix because no package mbeans are registered
//		Iterator it=getServer().queryNames(new ObjectName(ObjectNames.ALLSERVICES), null).iterator();
//		while(it.hasNext()){
//			super.testAttributes((ObjectName) it.next(),ManagedBundleMBean.class);
//		}
	}
    public void testUpdate()throws Exception{
//        ManagedBundleMBean bundle=(ManagedBundleMBean)MBeanServerInvocationHandler.newProxyInstance(getServer(), jmood, ManagedBundleMBean.class, false);
//        l.info("Last modified: "+bundle.getSymbolicName());
   }
    
}
