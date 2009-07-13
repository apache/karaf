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

package org.cauldron.sigil.internal.model.repository;

import org.cauldron.sigil.model.repository.IRepositoryType;
import org.eclipse.swt.graphics.Image;

public class RepositoryType implements IRepositoryType {
	private String type;
	private String id;
	private Image icon;
	private boolean dynamic;
	
	public RepositoryType(String id, String type, boolean dynamic,
			Image icon) {
		this.id = id;
		this.type = type;
		this.dynamic = dynamic;
		this.icon = icon;
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.ui.preferences.repository.IRepositoryType#getType()
	 */
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.ui.preferences.repository.IRepositoryType#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.ui.preferences.repository.IRepositoryType#getIcon()
	 */
	public Image getIcon() {
		return icon;
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.ui.preferences.repository.IRepositoryType#isDynamic()
	 */
	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public boolean equals(Object obj) {
		try {
			RepositoryType t = (RepositoryType) obj;
			return t.id.equals( id );
		}
		catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return type;
	}
}
