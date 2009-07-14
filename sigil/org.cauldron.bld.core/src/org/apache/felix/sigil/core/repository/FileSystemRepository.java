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

package org.apache.felix.sigil.core.repository;

import java.util.ArrayList;

import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.repository.AbstractBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryVisitor;
import org.eclipse.core.runtime.IPath;

public class FileSystemRepository extends AbstractBundleRepository {

	private ArrayList<ISigilBundle> bundles;
	private IPath path;
	private boolean recurse;
	
	public FileSystemRepository(String id, IPath path, boolean recurse) {
		super(id);
		this.path = path;
		this.recurse = recurse;
	}

	@Override
	public void accept(IRepositoryVisitor visitor, int options) {
		synchronized( this ) {
			if ( bundles == null ) {
				bundles = new ArrayList<ISigilBundle>();
				DirectoryHelper.scanBundles(this, bundles, path, null, recurse);
			}
		}
		
		for ( ISigilBundle b : bundles ) {
			if ( !visitor.visit(b) ) {
				break;
			}
		}		
	}

	public void refresh() {
		synchronized( this ) {
			bundles = null;
		}
		
		notifyChange();
	}

}
