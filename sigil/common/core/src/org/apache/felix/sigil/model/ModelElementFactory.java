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
import java.util.Map;

public abstract class ModelElementFactory {
	static class ElementInfo {
		Class<? extends IModelElement> implType;
		String name;
		String groupName;
		String groupURI;
		
		public ElementInfo(Class<? extends IModelElement> implType, String name, String groupName, String groupURI) {
			this.implType = implType;
			this.name = name;
			this.groupName = groupName;
			this.groupURI = groupURI;
		}
		
		public Class<? extends IModelElement> getImplType() {
			return implType;
		}
		public String getName() {
			return name;
		}
		
		public String getGroupName() {
			return groupName;
		}

		public String getGroupURI() {
			return groupURI;
		}
		
		public String toString() {
			return "ElementInfo[" + name + ":" + groupName + ":" + groupURI + ":" + implType.getCanonicalName() + "]";
		}
	}

	static class ModelInfo implements IModelInfo {

		private ElementInfo e;
		
		public ModelInfo(ElementInfo e) {
			this.e = e;
		}

		public String getGroupName() {
			return e.getGroupName();
		}

		public String getGroupURI() {
			return e.getGroupURI();
		}

		public String getName() {
			return e.getName();
		}

	}

	static class DefaultModelElementFactory extends ModelElementFactory {
		private HashMap<Class<? extends IModelElement>, ElementInfo> elementInfo = new HashMap<Class<? extends IModelElement>, ElementInfo>();
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends IModelElement> T newModelElement( Class<T> type ) throws ModelElementFactoryException {
			ElementInfo info = elementInfo.get( type );
			if ( info == null ) {
				throw new ModelElementFactoryException( "No implementation registered for " + type );
			}
			try {
				return (T) info.getImplType().newInstance();
			} catch (InstantiationException e) {
				throw new ModelElementFactoryException(e);
			} catch (IllegalAccessException e) {
				throw new ModelElementFactoryException(e);
			}
		}

		@Override
		public <T extends IModelElement> void register(Class<T> type, Class<? extends T> impl, String name, String groupName, String groupURI ) {
			elementInfo.put( type, new ElementInfo( impl, name, groupName, groupURI ) );
		}

		@Override
		public <T extends IModelElement> void unregister(Class<T> type,
				Class<? extends T> impl) {
			ElementInfo info = elementInfo.get( type );
			if ( info != null && info.getImplType() == impl ) {
				elementInfo.remove(type);
			}
		}

		@Override
		public IModelInfo getModelInfo(Class<? extends IModelElement> type) {
			ElementInfo e = findElementInfo( type );
			
			if ( e == null ) {
				return null;
			}
			
			return new ModelInfo( e );
		}

		@Override
		public IModelElement newModelElement(String namespaceURI, String localPart) throws ModelElementFactoryException {
			for ( Map.Entry<Class<? extends IModelElement>, ElementInfo> e : elementInfo.entrySet() ) {
				ElementInfo i = e.getValue();
				if ( equal( namespaceURI, i.getGroupURI() ) && equal( i.getName(), localPart ) ) {
					return newModelElement(e.getKey());
				}
			}
			
			return null;
		}

		private boolean equal(String val1, String val2) {
			return val1 == null ? val2 == null : val1.equals( val2 );
		}

		private ElementInfo findElementInfo( Class<? extends IModelElement> type ) {
			for ( ElementInfo e : elementInfo.values() ) {
				if ( e.getImplType() == type ) {
					return e;
				}
			}
			
			return null;
		}

	}

	public abstract <T extends IModelElement> T newModelElement( Class<T> type ) throws ModelElementFactoryException;
	
	public abstract IModelElement newModelElement(String namespaceURI, String localPart) throws ModelElementFactoryException;	
	
	public abstract <T extends IModelElement> void register( Class<T> type, Class<? extends T> impl, String name, String groupName, String groupURI );
	
	public abstract <T extends IModelElement> void unregister( Class<T> type, Class<? extends T> impl );

	public abstract IModelInfo getModelInfo( Class<? extends IModelElement> type );
	
	private static ModelElementFactory instance = new DefaultModelElementFactory();

	public static ModelElementFactory getInstance() {
		return instance;
	}
}
