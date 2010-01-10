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
package org.apache.felix.framework.security.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AllPermission;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.felix.moduleloader.IContent;
import org.osgi.framework.Bundle;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * A cache for local permissions. Local permissions are read from a given bundle
 * and cached for later lookup. See core spec 9.2.1.
 */
// TODO: maybe use bundle events to clean thing up or weak/soft references
public final class LocalPermissions
{
    private static final PermissionInfo[] ALL_PERMISSION = new PermissionInfo[] { new PermissionInfo(
        AllPermission.class.getName(), "", "") };

    private final Map m_cache = new WeakHashMap();
    private final Permissions m_permissions;

    public LocalPermissions(Permissions permissions) throws IOException
    {
        m_permissions = permissions;
    }

    /**
     * Return true in case that the given permission is implied by the local
     * permissions of the given bundle or if there are none otherwise, false.
     * See core spec 9.2.1.
     * 
     * @param root
     *            the root to use for cacheing as a key
     * @param loader
     *            the loader to get the content of the bundle from
     * @param bundle
     *            the bundle in quesiton
     * @param permission
     *            the permission to check
     * @return true if implied by local permissions.
     */
    public boolean implies(IContent content, Bundle bundle,
        Permission permission)
    {
        PermissionInfo[] permissions = null;

        synchronized (m_cache)
        {
            if (!m_cache.containsKey(content))
            {
                InputStream in = null;
                try
                {
                    in = content.getEntryAsStream("OSGI-INF/permissions.perm");
                    if (in != null)
                    {
                        ArrayList perms = new ArrayList();

                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in, "UTF-8"));
                        for (String line = reader.readLine(); line != null; line = reader
                            .readLine())
                        {
                            String trim = line.trim();
                            if (trim.startsWith("#") || trim.startsWith("//")
                                || (trim.length() == 0))
                            {
                                continue;
                            }
                            perms.add(new PermissionInfo(line));
                        }

                        permissions = (PermissionInfo[]) perms
                            .toArray(new PermissionInfo[perms.size()]);
                    }
                }
                catch (Exception ex)
                {
                }
                finally
                {
                    if (in != null)
                    {
                        try
                        {
                            in.close();
                        }
                        catch (IOException ex)
                        {
                            // TODO Auto-generated catch block
                            ex.printStackTrace();
                        }
                    }
                }

                if (permissions == null)
                {
                    permissions = ALL_PERMISSION;
                }

                m_cache.put(content, permissions);
            }
            else
            {
                permissions = (PermissionInfo[]) m_cache.get(content);
            }
        }

        return m_permissions.getPermissions(permissions).implies(permission,
            bundle);
    }
}
