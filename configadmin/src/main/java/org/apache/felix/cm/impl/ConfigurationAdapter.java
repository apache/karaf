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
package org.apache.felix.cm.impl;


import java.io.IOException;
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;


/**
 * The <code>ConfigurationAdapter</code> TODO
 *
 * @author fmeschbe
 * @version $Rev: 527592 $, $Date$
 */
public class ConfigurationAdapter implements Configuration
{

    private ConfigurationAdminImpl configurationAdmin;
    private ConfigurationImpl delegatee;


    ConfigurationAdapter( ConfigurationAdminImpl configurationAdmin, ConfigurationImpl delegatee )
    {
        this.configurationAdmin = configurationAdmin;
        this.delegatee = delegatee;
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#getPid()
     */
    public String getPid()
    {
        checkDeleted();
        return delegatee.getPid();
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#getFactoryPid()
     */
    public String getFactoryPid()
    {
        checkDeleted();
        return delegatee.getFactoryPid();
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#getBundleLocation()
     */
    public String getBundleLocation()
    {
        configurationAdmin.checkPermission();
        checkDeleted();
        return delegatee.getBundleLocation();
    }


    /**
     * @param bundleLocation
     * @see org.apache.felix.cm.impl.ConfigurationImpl#setBundleLocation(java.lang.String)
     */
    public void setBundleLocation( String bundleLocation )
    {
        configurationAdmin.checkPermission();
        checkDeleted();
        delegatee.setBundleLocation( bundleLocation );
    }


    /**
     * @throws IOException
     * @see org.apache.felix.cm.impl.ConfigurationImpl#update()
     */
    public void update() throws IOException
    {
        checkDeleted();
        delegatee.update();
    }


    /**
     * @param properties
     * @throws IOException
     * @see org.apache.felix.cm.impl.ConfigurationImpl#update(java.util.Dictionary)
     */
    public void update( Dictionary properties ) throws IOException
    {
        checkDeleted();
        delegatee.update( properties );
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#getProperties()
     */
    public Dictionary getProperties()
    {
        checkDeleted();
        return delegatee.getProperties();
    }


    /**
     * @throws IOException
     * @see org.apache.felix.cm.impl.ConfigurationImpl#delete()
     */
    public void delete() throws IOException
    {
        checkDeleted();
        delegatee.delete();
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#hashCode()
     */
    public int hashCode()
    {
        return delegatee.hashCode();
    }


    /**
     * @param obj
     * @see org.apache.felix.cm.impl.ConfigurationImpl#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        return delegatee.equals( obj );
    }


    /**
     * @see org.apache.felix.cm.impl.ConfigurationImpl#toString()
     */
    public String toString()
    {
        return delegatee.toString();
    }

    /**
     * Checks whether this configuration object has already been deleted.
     *
     * @throws IllegalStateException If this configuration object has been
     *      deleted.
     */
    private void checkDeleted() {
        if (delegatee.isDeleted()) {
            throw new IllegalStateException( "Configuration " + delegatee.getPid() + " deleted" );
        }
    }
}
