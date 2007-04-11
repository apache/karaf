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
package org.apache.felix.metatype;


/**
 * The <code>Designate</code> class represents the <code>Designate</code>
 * element of the meta type descriptor.
 * 
 * @author fmeschbe
 */
public class Designate
{

    private String pid;

    private String factoryPid;

    private String bundleLocation;

    private boolean optional;

    private boolean merge;

    private DesignateObject object;


    /**
     * @return the bundleLocation
     */
    public String getBundleLocation()
    {
        return bundleLocation;
    }


    /**
     * @return the factoryPid
     */
    public String getFactoryPid()
    {
        return factoryPid;
    }


    /**
     * @return the merge
     */
    public boolean isMerge()
    {
        return merge;
    }


    /**
     * @return the optional
     */
    public boolean isOptional()
    {
        return optional;
    }


    /**
     * @return the pid
     */
    public String getPid()
    {
        return pid;
    }


    /**
     * @return the object
     */
    public DesignateObject getObject()
    {
        return object;
    }


    /**
     * @param bundleLocation the bundleLocation to set
     */
    public void setBundleLocation( String bundleLocation )
    {
        this.bundleLocation = bundleLocation;
    }


    /**
     * @param factoryPid the factoryPid to set
     */
    public void setFactoryPid( String factoryPid )
    {
        this.factoryPid = factoryPid;
    }


    /**
     * @param merge the merge to set
     */
    public void setMerge( boolean merge )
    {
        this.merge = merge;
    }


    /**
     * @param optional the optional to set
     */
    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }


    /**
     * @param pid the pid to set
     */
    public void setPid( String pid )
    {
        this.pid = pid;
    }


    /**
     * @param object the object to set
     */
    public void setObject( DesignateObject object )
    {
        this.object = object;
    }
}
