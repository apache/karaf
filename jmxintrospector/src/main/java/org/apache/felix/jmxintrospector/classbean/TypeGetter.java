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

package org.apache.felix.jmxintrospector.classbean;

import java.util.Collection;

import sun.security.krb5.internal.tools.Klist;

import javassist.ClassPool;
import javassist.CtClass;

public class TypeGetter implements TypeGetterMBean {

	public Class getType(String name) throws Exception{
		CtClass cl=ClassPool.getDefault().get(name);
		Collection classes=cl.getRefClasses();
		CtClass[] all=ClassPool.getDefault().get((String[])classes.toArray(new String[classes.size()]));
		for (CtClass klazz : all) {
			klazz.toBytecode();
			
		} 
		ClassLoader loader=this.getClass().getClassLoader();
		try {
			return  loader.loadClass(name);
		} catch (ClassNotFoundException e) {
			return null; //FIXME: should we implement an searching mechanism?
			}
	}
	
}
