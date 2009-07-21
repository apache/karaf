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

package org.apache.felix.sigil.ivy;


import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.core.BldCore;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.repository.IResolution;
import org.apache.felix.sigil.repository.IResolutionMonitor;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.apache.felix.sigil.repository.ResolutionException;


public class BldResolver implements IBldResolver
{
    private Map<String, Properties> repos;
    private BldRepositoryManager manager;

    static
    {
        try
        {
            BldCore.init();
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    };


    public BldResolver( Map<String, Properties> repos )
    {
        this.repos = repos;
    }


    public IResolution resolve( IModelElement element, boolean transitive )
    {
        int options = ResolutionConfig.IGNORE_ERRORS | ResolutionConfig.INCLUDE_OPTIONAL;
        if ( transitive )
            options |= ResolutionConfig.INCLUDE_DEPENDENTS;

        ResolutionConfig config = new ResolutionConfig( options );
        try
        {
            return resolve( element, config );
        }
        catch ( ResolutionException e )
        {
            throw new IllegalStateException( "eek! this shouldn't happen when ignoreErrors=true", e );
        }
    }


    public IResolution resolveOrFail( IModelElement element, boolean transitive ) throws ResolutionException
    {
        int options = 0;
        if ( transitive )
            options |= ResolutionConfig.INCLUDE_DEPENDENTS;
        ResolutionConfig config = new ResolutionConfig( options );
        return resolve( element, config );
    }


    private IResolution resolve( IModelElement element, ResolutionConfig config ) throws ResolutionException
    {
        if ( manager == null )
        {
            manager = new BldRepositoryManager( repos );
        }

        IResolutionMonitor nullMonitor = new IResolutionMonitor()
        {
            public void endResolution( IModelElement requirement, ISigilBundle sigilBundle )
            {
            }


            public boolean isCanceled()
            {
                return false;
            }


            public void startResolution( IModelElement requirement )
            {
            }
        };

        return manager.getBundleResolver().resolve( element, config, nullMonitor );
    }
}
