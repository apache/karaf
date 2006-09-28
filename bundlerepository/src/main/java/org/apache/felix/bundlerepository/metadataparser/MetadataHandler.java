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
package org.apache.felix.bundlerepository.metadataparser;

import java.io.InputStream;
import java.lang.reflect.Method;

public abstract class MetadataHandler {

	protected XmlCommonHandler handler;

	/**
	 * constructor 
	 *
	 */
	public MetadataHandler() {
		handler = new XmlCommonHandler();
	}

	/**
	* Called to parse the InputStream and set bundle list and package hash map
	*/
	public abstract void parse(InputStream is) throws Exception;

	/**
	 * return the metadata after the parsing
	 * @return a Object. Its class is the returned type of instanceFactory newInstance method for the root element of the XML document.
	 */
	public final Object getMetadata() {
		return handler.getRoot();
	}
	/**
	 * Add a type for a element
	 * @param qname the name of the element to process
	 * @param instanceFactory the factory of objects representing an element. Must have a newInstance method. could be a class.
	 * @throws Exception
	 */
	public final void addType(String qname, Object instanceFactory) throws Exception {
		handler.addType(qname, instanceFactory, null, null);
	}

	/**
	 * Add a type for a element
	 * @param qname the name of the element to process
	 * @param instanceFactory the factory of objects representing an element. Must have a newInstance method. could be a class.
	 * @param castClass the class used to introspect the adder/setter and parameters in parent adder/setter. if null the castClass is by default the class returned by the newInstance method of the instanceFactory.
	 * @throws Exception
	 */
	public final void addType(String qname, Object instanceFactory, Class castClass) throws Exception {
		handler.addType(qname, instanceFactory, castClass, null);
	}

	/**
	 * Add a type for a element
	 * @param qname the name of the element to process
	 * @param instanceFactory the factory of objects representing an element. Must have a newInstance method. could be a class.
	 * @param castClass the class used to introspect the adder/setter and parameters in parent adder/setter. if null the castClass is by default the class returned by the newInstance method of the instanceFactory.
	 * @param defaultAddMethod the method used to add the sub-elements and attributes if no adder/setter is founded. could be omitted.
	 * @throws Exception
	 */
	public final void addType(String qname, Object instanceFactory, Class castClass, Method defaultAddMethod) throws Exception {
		handler.addType(qname, instanceFactory, castClass, defaultAddMethod);
	}
	
	/**
	 * Add a type for the default element
	 * @param instanceFactory the factory of objects representing an element. Must have a newInstance method. could be a class.
	 * @throws Exception
	 */
	public final void setDefaultType(Object instanceFactory) throws Exception {
		handler.setDefaultType(instanceFactory,null,null);
	}

	/**
	 * Add a type for the default element
	 * @param instanceFactory the factory of objects representing an element. Must have a newInstance method. could be a class.
	 * @param castClass the class used to introspect the adder/setter and parameters in parent adder/setter. if null the castClass is by default the class returned by the newInstance method of the instanceFactory.
	 * @throws Exception
	 */
	public final void setDefaultType(Object instanceFactory, Class castClass) throws Exception {
		handler.setDefaultType(instanceFactory, castClass,null);
	}

	/**
	 * Add a type for the default element
	 * @param instanceFactory the factory of objects representing an element. Must have a newInstance method. could be a class.
	 * @param castClass the class used to introspect the adder/setter and parameters in parent adder/setter. if null the castClass is by default the class returned by the newInstance method of the instanceFactory.
	 * @param defaultAddMethod the method used to add the sub-elements and attributes if no adder/setter is founded. could be omitted.
	 * @throws Exception
	 */
	public final void setDefaultType(Object instanceFactory, Class castClass, Method defaultAddMethod) throws Exception {
		handler.setDefaultType(instanceFactory,castClass,defaultAddMethod);
	}

	/**
	 * Add a type to process the processing instruction
	 * @param piname
	 * @param clazz
	 */
	public final void addPI(String piname, Class clazz) {
		handler.addPI(piname, clazz);
	}

	/**
	 * set the missing PI exception flag. If during parsing, the flag is true and the processing instruction is unknown, then the parser throws a exception  
	 * @param flag
	 */
	public final void setMissingPIExceptionFlag(boolean flag) {
		handler.setMissingPIExceptionFlag(flag);
	}

	/**
	 * 
	 * @param trace
	 * @since 0.9.1
	 */
	public final void setTrace(boolean trace) {
		handler.setTrace(trace);
	}
}
