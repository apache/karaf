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
package org.apache.felix.dm.runtime;

import org.osgi.service.log.LogService;

/**
 * This class logs some formattable strings into the OSGi Log Service.
 */
public class Log
{
    /** The log service */
    private LogService m_logService;
    
    /** Our sole instance */
    private static Log m_instance = new Log();
    
    public static Log instance()
    {
        return m_instance;
    }
    
    public void setLogService(LogService logService) {
        m_logService = logService;
    }
        
    public void log(int level, String format, Object ... args) 
    {
        m_logService.log(level, String.format(format, args));
    }
    
    public void log(int level, String format, Throwable t, Object ... args) 
    {
        m_logService.log(level, String.format(format, args), t);
    }
}
