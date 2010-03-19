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
package org.apache.felix.bundlerepository.impl;

import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.apache.felix.bundlerepository.Repository;

public class SystemRepositoryImpl implements Repository
{

    private final Logger m_logger;
    private final long lastModified;
    private final LocalResourceImpl systemBundleResource;

    public SystemRepositoryImpl(BundleContext context, Logger logger)
    {
        m_logger = logger;
        lastModified = System.currentTimeMillis();
        try
        {
            systemBundleResource = new LocalResourceImpl(context.getBundle(0));
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen since we are generating filters,
            // but ignore the resource if it does occur.
            m_logger.log(Logger.LOG_WARNING, ex.getMessage(), ex);
            throw new IllegalStateException("Unexpected error", ex);
        }
    }

    public String getURI()
    {
        return SYSTEM;
    }

    public Resource[] getResources()
    {
        return new Resource[] { systemBundleResource };
    }

    public String getName()
    {
        return "System Repository";
    }

    public long getLastModified()
    {
        return lastModified;
    }

}