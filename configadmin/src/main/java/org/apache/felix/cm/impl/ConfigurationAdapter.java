/*
 * $Url: $
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
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
