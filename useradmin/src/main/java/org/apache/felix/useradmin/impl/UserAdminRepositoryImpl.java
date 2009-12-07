/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;

import org.apache.felix.useradmin.UserAdminRepository;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * UserAdminRepository implementation of {@link UserAdminRepository}.
 * 
 * @version $Rev$ $Date$
 */
public class UserAdminRepositoryImpl implements UserAdminRepository
{
    /**
     * UserAdmin repository cache, caching all roles during runtime.
     */
    private Hashtable repositoryCache;
    /**
     * Store file name.
     */
    private String repositoryFileName;
    /**
     * Store file.
     */
    private File repositoryFile;
    /**
     * Property pointing out the file containing the role database information.
     */
    private final static String DBPROP = "org.apache.felix.useradmin.db";
    private Logger logger;

    /**
     * Constructs new UserAdminRepository.
     * @param logger Logger instance.
     * @param context bundle context.
     */
    public UserAdminRepositoryImpl(Logger logger, BundleContext context)
    {
        this.repositoryFileName = System.getProperty(DBPROP, "useradmin.db");
        this.logger = logger;
        this.repositoryFile = context.getDataFile(repositoryFileName);
    }

    /**
     * @see org.apache.felix.useradmin.UserAdminRepository#load()
     */
    public void load()
    {
        try
        {
            logger.log(LogService.LOG_DEBUG, "Loading User Admin Repository");

            if (repositoryFile == null || !repositoryFile.exists())
            {
                repositoryFile = new File(repositoryFileName);
            }

            if (repositoryFile != null && repositoryFile.exists())
            {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(repositoryFile));
                Object obj = ois.readObject();
                ois.close();
                if (obj instanceof Hashtable)
                {
                    repositoryCache = (Hashtable) obj;
                    logger.log(LogService.LOG_INFO, "User Admin Repository loaded");
                }
                else
                {
                    logger.log(LogService.LOG_ERROR, "User Admin Repository corrupted");
                }
            }
            else
            {
                logger.log(LogService.LOG_DEBUG, "User Admin Repository not found ");
            }
        }
        catch (IOException e)
        {
            logger.log(LogService.LOG_ERROR, "Can't load User Admin Repository", e);
        }
        catch (ClassNotFoundException e)
        {
            logger.log(LogService.LOG_ERROR, "Can't load User Admin Repository", e);
        }

        if (repositoryCache == null)
        {
            repositoryCache = new Hashtable();
        }
    }

    /**
     * @see org.apache.felix.useradmin.UserAdminRepository#flush()
     */
    public void flush()
    {

        try
        {
            if (repositoryFile != null)
            {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(repositoryFile));
                oos.writeObject(repositoryCache);
                oos.close();
            }
            else
            {
                logger.log(LogService.LOG_DEBUG, "User Admin Repository not found ");
            }
        }
        catch (IOException e)
        {
            logger.log(LogService.LOG_ERROR, "Failed to save roles", e);
        }

    }

    /**
     * @see org.apache.felix.useradmin.UserAdminRepository#getRepositoryCache()
     */
    public Hashtable getRepositoryCache()
    {
        return repositoryCache;
    }
}
