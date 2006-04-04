/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.bundlerepository.metadataparser;

import java.io.InputStream;


/**
 * @version 	1.00 9 Jul 2004
 * @author 	Didier Donsez
 */
public abstract class MetadataHandler {

	protected XmlCommonHandler handler;

	public MetadataHandler() {
		handler = new XmlCommonHandler();
	}

	/**
	* Called to parse the InputStream and set bundle list and package hash map
	*/
	public abstract void parse(InputStream is) throws Exception;

	/**
	 * return the metadata
	 * @return a Objet
	 */
	public final Object getMetadata() {
		return handler.getRoot();
	}

	public final void addType(String qname, Object instanceFactory) throws Exception {
		handler.addType(qname, instanceFactory, null);
	}

	public final void addType(String qname, Object instanceFactory, Class castClass) throws Exception {
		handler.addType(qname, instanceFactory, castClass);
	}
	
	public final void setDefaultType(Object instanceFactory) throws Exception {
		handler.setDefaultType(instanceFactory,null);
	}

	public final void setDefaultType(Object instanceFactory, Class castClass) throws Exception {
		handler.setDefaultType(instanceFactory, castClass);
	}

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
