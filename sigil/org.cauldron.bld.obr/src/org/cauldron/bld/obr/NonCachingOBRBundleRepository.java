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

package org.cauldron.bld.obr;

import java.io.File;
import java.net.URL;

import org.cauldron.bld.core.BldCore;
import org.cauldron.sigil.model.eclipse.ISigilBundle;
import org.cauldron.sigil.repository.IRepositoryVisitor;

public class NonCachingOBRBundleRepository extends AbstractOBRBundleRepository {

	public static void main(String[] args) throws Exception {
		String url = args[0];
		String obr = args[1];
		String cache = args[2];
		String update = args[3];
		BldCore.init();
		NonCachingOBRBundleRepository rep = new NonCachingOBRBundleRepository( "main", new URL(url), new File(obr), new File(cache), Long.parseLong(update));
		rep.accept(new IRepositoryVisitor() {
			public boolean visit(ISigilBundle bundle) {
				System.out.println( "Found " + bundle );
				return true;
			}
		});
	}
	
	public NonCachingOBRBundleRepository(String id, URL repositoryURL, File obrCache, File bundleCache, long updatePeriod) {
		super(id, repositoryURL, obrCache, bundleCache, updatePeriod);
	}

	@Override
	public void accept(final IRepositoryVisitor visitor, int options) {
		try {
			readBundles(new OBRListener() {
				boolean visit = true;
				public void handleBundle(ISigilBundle bundle) {
					if ( visit ) {
						visit = visitor.visit(bundle);
					}
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
