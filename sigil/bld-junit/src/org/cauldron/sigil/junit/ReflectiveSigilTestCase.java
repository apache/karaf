/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.cauldron.sigil.junit;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class ReflectiveSigilTestCase extends AbstractSigilTestCase {

	private Class<?>[] references;
	private Map<Class<?>, Method> bindMethods;
	private Map<Class<?>, Method> unbindMethods;
	
	@Override
	protected Class<?>[] getReferences() {
		introspect();
		return references;
	}

	@Override
	protected Method getBindMethod(Class<?> clazz) {
		return bindMethods.get(clazz);
	}

	@Override
	protected Method getUnbindMethod(Class<?> clazz) {
		return unbindMethods.get(clazz);
	}

	private void introspect() {
		if ( references == null ) {
			bindMethods = findBindMethods(getClass(), "set", "add");
			unbindMethods = findBindMethods(getClass(), "set", "remove");
			
			HashSet<Class<?>> refs = new HashSet<Class<?>>();
			refs.addAll( bindMethods.keySet() );
			refs.addAll( unbindMethods.keySet() );
			references = refs.toArray( new Class<?>[refs.size()] );
		}
	}

	private static Map<Class<?>, Method> findBindMethods(Class<?> clazz, String... prefix) {
		HashMap<Class<?>, Method> found = new HashMap<Class<?>, Method>();
		
		checkDeclaredMethods(clazz, found, prefix);
		
		return found;
	}
	
	private static void checkDeclaredMethods(Class<?> clazz, Map<Class<?>, Method> found, String...prefix) {
		for ( Method m : clazz.getDeclaredMethods() ) {
			if ( isMethodPrefixed(m, prefix) && isBindSignature(m) ) {
				found.put( m.getParameterTypes()[0], m );
			}
		}
		
		Class<?> sup = clazz.getSuperclass();
		if ( sup != null && sup != Object.class ) {
			checkDeclaredMethods(sup, found, prefix);
		}
		
		for ( Class<?> i : clazz.getInterfaces() ) {
			checkDeclaredMethods(i, found, prefix);
		}
	}

	private static boolean isMethodPrefixed(Method m, String...prefix) {
		String n = m.getName();
		for ( String p : prefix ) {
			if ( n.startsWith( p ) && n.length() > p.length() ) {
				return true;
			}
		}
		return false;
	}
	private static boolean isBindSignature(Method m) {
		return m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 1;
	}
}
