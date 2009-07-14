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

package org.apache.felix.sigil.eclipse.internal.model.repository;

import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;
import org.eclipse.jface.preference.PreferenceStore;

public class RepositoryModel implements IRepositoryModel {
	private String id;
	
	private String name;
	
	private IRepositoryType type;
	
	private PreferenceStore preferences;
		
	public RepositoryModel(String id, String name, IRepositoryType type, PreferenceStore preferences) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.preferences = preferences;
	}
	
	public PreferenceStore getPreferences() {
		return preferences;
	}

	public IRepositoryType getType() {
		return type;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		try {
			RepositoryModel e = (RepositoryModel) obj;
			return id.equals(e.id);
		}
		catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	public String toString() {
		return type.getId() + ":" + id + ":" + name;
	}
}