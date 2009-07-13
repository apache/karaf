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

package org.cauldron.bld.ivy;

import java.util.HashMap;
import java.util.Properties;

import org.cauldron.sigil.repository.IBundleRepository;
import org.cauldron.sigil.repository.IRepositoryProvider;
import org.cauldron.sigil.repository.RepositoryException;

public class ProjectRepositoryProvider implements IRepositoryProvider{
	private static HashMap<String, ProjectRepository> cache = new HashMap<String, ProjectRepository>();
	
	public IBundleRepository createRepository(String id, Properties properties) throws RepositoryException {
		ProjectRepository repository = cache.get(id);
		
		if (repository == null) {
    		String pattern = properties.getProperty("pattern");
    		if (pattern == null) {
    		    throw new RepositoryException("property 'pattern' not specified.");
    		}
            repository = new ProjectRepository(id, pattern);
            cache.put(id, repository);
		}
		
        return repository;
    }
}
