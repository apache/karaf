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

package org.apache.felix.sigil.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class BldProperties extends Properties
{
    private static final long serialVersionUID = 1L;
    private static final BldProperties global = new BldProperties(null, null);
    private static final Properties sysEnv;

    static
    {
        Properties env = new Properties();
        env.putAll(System.getenv());
        sysEnv = new Properties(env);
        sysEnv.putAll(System.getProperties());
        // Note: these are System properties, NOT Ant properties.
    }

    private final Properties mySysEnv;
    
    BldProperties(File baseDir, Properties overrides)
    {
        mySysEnv = new Properties(sysEnv);
        
        if (overrides != null)
        {
            mySysEnv.putAll(overrides);
        }
        
        try
        {
            if (baseDir != null)
            {
                mySysEnv.setProperty(".", baseDir.getCanonicalPath());
                mySysEnv.setProperty("..", baseDir.getParentFile().getCanonicalPath());
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getProperty(String key, String defaultValue)
    {
        return mySysEnv.getProperty(key, defaultValue);
    }

    public String getProperty(String key)
    {
        return mySysEnv.getProperty(key);
    }

    public static Properties global()
    {
        return global;
    }
}
