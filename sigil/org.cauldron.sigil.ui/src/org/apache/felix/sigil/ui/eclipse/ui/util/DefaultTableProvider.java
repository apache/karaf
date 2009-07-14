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

package org.apache.felix.sigil.ui.eclipse.ui.util;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;

public abstract class DefaultTableProvider extends DefaultContentProvider implements IStructuredContentProvider {

	/**
	 * Utility method to convert the input element to an Object[].
	 * 
	 * @param inputElement
	 * 
	 * @return if inputElement is null -> empty array <br/>
	 *         if inputElement is a {@link Collection} returns {@link Collection#toArray()}<br/>
	 *         if inputElement is an Array class cast of inputElement to Object[]<br/>
	 *  
	 * @throws IllegalArgumentException if the element cannot be converted. 
	 */
	public Object[] toArray(Object inputElement) {
		if ( inputElement == null ) {
			return new Object[] {};
		}
		else if ( inputElement instanceof Collection ) {
			Collection<?> col = (Collection<?>) inputElement;
			return col.toArray();
		}
		else if ( inputElement.getClass().isArray() ) {
			return (Object[]) inputElement;
		}
		else {
			throw new IllegalArgumentException( "Invalid inputElement " + inputElement.getClass() );
		}		
	}
	
}
