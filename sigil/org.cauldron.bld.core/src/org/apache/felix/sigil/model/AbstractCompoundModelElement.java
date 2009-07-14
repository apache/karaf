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

package org.apache.felix.sigil.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class AbstractCompoundModelElement extends AbstractModelElement implements ICompoundModelElement {

	private static final long serialVersionUID = 1L;
	
	public AbstractCompoundModelElement(String description) {
		super(description);
	}

	public boolean addChild(IModelElement child) throws InvalidModelException {
		return support.addChild(child);
	}

	public boolean removeChild(IModelElement child) {
		return support.removeChild(child);
	}

	public IModelElement[] children() {
		return support.children();
	}
	
	private static final ThreadLocal<Map<IModelWalker,Set<IModelElement>>> walkedLocal = new ThreadLocal<Map<IModelWalker,Set<IModelElement>>>();
	
	public void visit(IModelWalker walker) {
		if ( walker.visit( this ) ) {
			Map<IModelWalker,Set<IModelElement>> walked = walkedLocal.get();
			boolean delete = false;
			
			if ( walked == null ) {
				walked = new HashMap<IModelWalker, Set<IModelElement>>();
				walkedLocal.set(walked);
			}

			Set<IModelElement> check = walked.get(walker);
			
			if ( check == null ) {
				delete = true;
				check = new HashSet<IModelElement>(); 
			}
			
			check.add( this );
			
			try {
				for ( IModelElement e : children() ) {
					if ( !check.contains( e ) && walker.visit( e ) ) {
						check.add( e );
						if ( e instanceof ICompoundModelElement ) {
							ICompoundModelElement c = (ICompoundModelElement) e;
							c.visit(walker);
						}
					}
				}
			}
			finally {
				if ( delete ) {
					walked.remove(walker);
					
					if ( walked.isEmpty() ) {
						walkedLocal.set( null );
					}
				}
			}
		}
	}

	public Set<Class<? extends IModelElement>> getOptionalChildren() {
		return support.getChildrenTypes(false);
	}

	public Set<Class<? extends IModelElement>> getRequiredChildren() {
		return support.getChildrenTypes(true);
	}

	public <T extends IModelElement> T[] childrenOfType(Class<T> type) {
		return support.childrenOfType( type );
	}


	@Override
	public void checkValid() throws InvalidModelException {
		super.checkValid();
		
		for ( IModelElement e : support.children() ) {
			e.checkValid();
		}
	}	
}
