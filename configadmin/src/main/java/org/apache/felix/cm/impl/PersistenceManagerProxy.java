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
import java.util.Enumeration;

import org.apache.felix.cm.PersistenceManager;


/**
 * The <code>PersistenceManagerProxy</code> TODO
 *
 * @author fmeschbe
 */
class PersistenceManagerProxy implements PersistenceManager
{

    private PersistenceManager delegatee;


    PersistenceManagerProxy( PersistenceManager delegatee )
    {
        setPersistenceManager( delegatee );
    }


    /**
     * @param pid
     * @throws IOException
     * @see org.apache.felix.cm.PersistenceManager#delete(java.lang.String)
     */
    public void delete( String pid ) throws IOException
    {
        checkDelegatee();
        delegatee.delete( pid );
    }


    /**
     * @param pid
     * @see org.apache.felix.cm.PersistenceManager#exists(java.lang.String)
     */
    public boolean exists( String pid )
    {
        return delegatee != null && delegatee.exists( pid );
    }


    /**
     * @throws IOException
     * @see org.apache.felix.cm.PersistenceManager#getDictionaries()
     */
    public Enumeration getDictionaries() throws IOException
    {
        checkDelegatee();
        return delegatee.getDictionaries();
    }


    /**
     * @param pid
     * @throws IOException
     * @see org.apache.felix.cm.PersistenceManager#load(java.lang.String)
     */
    public Dictionary load( String pid ) throws IOException
    {
        checkDelegatee();
        return delegatee.load( pid );
    }


    /**
     * @param pid
     * @param properties
     * @throws IOException
     * @see org.apache.felix.cm.PersistenceManager#store(java.lang.String, java.util.Dictionary)
     */
    public void store( String pid, Dictionary properties ) throws IOException
    {
        checkDelegatee();
        delegatee.store( pid, properties );
    }


    void setPersistenceManager( PersistenceManager delegatee )
    {
        this.delegatee = delegatee;
    }


    void checkDelegatee() throws IOException
    {
        if ( delegatee == null )
        {
            throw new IOException( "PersistenceManager not valid" );
        }
    }
}
