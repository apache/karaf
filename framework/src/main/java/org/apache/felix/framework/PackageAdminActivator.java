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
package org.apache.felix.framework;

import org.osgi.framework.*;

class PackageAdminActivator implements BundleActivator
{
    private Felix m_felix = null;
    private ServiceRegistration m_reg = null;
    private PackageAdminImpl m_packageAdmin = null;

    public PackageAdminActivator(Felix felix)
    {
        m_felix = felix;
    }

    public void start(BundleContext context) throws Exception
    {
        m_packageAdmin = new PackageAdminImpl(m_felix);
        m_reg = context.registerService(
            org.osgi.service.packageadmin.PackageAdmin.class.getName(),
            m_packageAdmin, null);
    }

    public void stop(BundleContext context) throws Exception
    {
        m_reg.unregister();
        m_packageAdmin.stop();
    }
}