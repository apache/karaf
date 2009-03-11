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
package org.apache.felix.fileinstall;

public class ConfigurationKey
{
    private String factoryId;
    private String pid;

    public ConfigurationKey(String factoryId, String pid)
    {
        this.factoryId = factoryId;
        this.pid = pid;
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((factoryId == null) ? 0 : factoryId.hashCode());
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        ConfigurationKey other = (ConfigurationKey) obj;
        if (factoryId == null)
        {
            if (other.factoryId != null)
            {
                return false;
            }
        }
        else if (!factoryId.equals(other.factoryId))
        {
            return false;
        }
        if (pid == null)
        {
            if (other.pid != null)
            {
                return false;
            }
        }
        else if (!pid.equals(other.pid))
        {
            return false;
        }
        return true;
    }
}