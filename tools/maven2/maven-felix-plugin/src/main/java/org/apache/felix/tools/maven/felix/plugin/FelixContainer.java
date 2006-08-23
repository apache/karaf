/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.tools.maven.felix.plugin;


import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;


public class FelixContainer extends Felix
{
    public Bundle[] getBundles()
    {
        return super.getBundles();
    }

    public Bundle getBundle( String location )
    {
        return super.getBundle( location );
    }
    
    public BundleActivator getBundleActivator( Bundle bundle )
    {
        return super.getBundleActivator( bundle );
    }
}
