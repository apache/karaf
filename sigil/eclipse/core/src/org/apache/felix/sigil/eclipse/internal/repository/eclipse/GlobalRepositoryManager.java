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

package org.apache.felix.sigil.eclipse.internal.repository.eclipse;


import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.internal.repository.eclipse.SigilRepositoryManager;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.repository.IRepositoryManager;


public class GlobalRepositoryManager extends SigilRepositoryManager implements IRepositoryManager
{

    public GlobalRepositoryManager(RepositoryMap map)
    {
        super( null, map );
    }


    @Override
    protected IRepositoryModel[] findRepositories()
    {
        List<IRepositoryModel> repos = SigilCore.getRepositoryConfiguration().loadRepositories();
        return repos.toArray( new IRepositoryModel[repos.size()] );
    }

}
