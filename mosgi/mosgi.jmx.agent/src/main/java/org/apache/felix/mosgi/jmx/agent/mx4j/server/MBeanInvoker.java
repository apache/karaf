/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.agent.mx4j.server;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

/**
 * Invokes methods on standard MBeans. <p>
 * Actually two implementations are available: one that uses reflection and one that generates on-the-fly a customized
 * MBeanInvoker per each particular MBean and that is implemented with direct calls. <br>
 * The default is the direct call version, that uses the <a href="http://jakarta.apache.org/bcel">BCEL</a> to generate
 * the required bytecode on-the-fly. <br>
 * In the future may be the starting point for MBean interceptors.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface MBeanInvoker
{
	/**
	 * Invokes the specified operation on the MBean instance
	 */
    public Object invoke(MBeanMetaData metadata, String method, String[] signature, Object[] args) throws MBeanException, ReflectionException;
	/**
	 * Returns the value of the specified attribute.
	 */
	public Object getAttribute(MBeanMetaData metadata, String attribute) throws MBeanException, AttributeNotFoundException, ReflectionException;
	/**
	 * Sets the value of the specified attribute.
	 */
	public void setAttribute(MBeanMetaData metadata, Attribute attribute) throws MBeanException, AttributeNotFoundException, InvalidAttributeValueException, ReflectionException;
}
