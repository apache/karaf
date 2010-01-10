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
package org.apache.felix.framework;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.felix.framework.ext.SecurityProvider;
import org.apache.felix.framework.security.SecurityConstants;
import org.apache.felix.framework.security.condpermadmin.ConditionalPermissionAdminImpl;
import org.apache.felix.framework.security.permissionadmin.PermissionAdminImpl;
import org.apache.felix.framework.security.util.Conditions;
import org.apache.felix.framework.security.util.LocalPermissions;
import org.apache.felix.framework.security.util.Permissions;
import org.apache.felix.framework.security.util.PropertiesCache;
import org.apache.felix.framework.util.SecureAction;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;

/**
 * <p>
 * This Felix specific activator installs a security provider with the Felix
 * framework. The security settings can be changed via the
 * {@link PermissionAdmin} and/or the {@link ConditionalPermissionAdmin}
 * services that may be published by this class.
 * </p>
 * <p>
 * Permission informations as well as caching data will be stored in several
 * files in a directory called <tt>security</tt> obtained by a call to
 * {@link BundleContext#getDataFile(String))}.
 * </p>
 * <p>
 * The following properties are recognized:
 * <p>
 * {@link SecurityConstants#ENABLE_PERMISSIONADMIN_PROP} - Whether or not (
 * <tt>true</tt>|<tt>false</tt>) to publish a{@link ConditionalPermissionAdmin}
 * service. The default is
 * {@link SecurityConstants#ENABLE_PERMISSIONADMIN_VALUE}.
 * </p>
 * <p>
 * {@link SecurityConstants#ENABLE_CONDPERMADMIN_PROP} - Whether or not (
 * <tt>true</tt>|<tt>false</tt>) to publish a{@link ConditionalPermissionAdmin}
 * service. The default is {@link SecurityConstants#ENABLE_CONDPERMADMIN_VALUE}.
 * </p>
 * <p>
 * {@link SecurityConstants#KEYSTORE_FILE_PROP} - The keystore URL(s) to use as
 * trusted CA stores. The urls must be separated by a guard (i.e., <tt>|</tt>).
 * The default is {@link SecurityConstants#KEYSTORE_FILE_VALUE}.
 * </p>
 * <p>
 * {@link SecurityConstants#KEYSTORE_PASS_PROP} - The keystore password(s) to
 * use for the given keystores. The passwords must be separated by a guard
 * (i.e., <tt>|</tt>).The default is
 * {@link SecurityConstants#KEYSTORE_PASS_VALUE}.
 * </p>
 * <p>
 * {@link SecurityConstants#KEYSTORE_TYPE_PROP} - The keystore type(s) to use
 * for the given keystores. The types must be separated by a guard (i.e.,
 * <tt>|</tt>).The default is {@link SecurityConstants#KEYSTORE_TYPE_VALUE}.
 * </p>
 * <p>
 * {@link SecurityConstants#CRL_FILE_PROP} - The CRL URL(s) to use for revoked
 * certificates. The urls must be separated by a guard (i.e., <tt>|</tt>). The
 * default is {@link SecurityConstants#CRL_FILE_VALUE}.
 * </p>
 * </p>
 */
/*
 * TODO: using a string for passwords is bad. We need to investigate
 * alternatives.
 * 
 * TODO: we might want to allow for the recognized properties to change without
 * a restart. This is trick because we can not publish a managed service due to
 * not being able to import as we are an extension bundle.
 */
public final class SecurityActivator implements BundleActivator
{
    public synchronized void start(BundleContext context) throws Exception
    {
        PermissionAdminImpl pai = null;

        SecureAction action = new SecureAction();

        Permissions permissions = new Permissions(context, action);

        File tmp = context.getDataFile("security" + File.separator + "tmp");
        if ((tmp == null) || (!tmp.isDirectory() && !tmp.mkdirs()))
        {
            throw new IOException("Can't create tmp dir.");
        }
        // TODO: log something if we can not clean-up the tmp dir
        File[] old = tmp.listFiles();
        if (old != null)
        {
            for (int i = 0; i < old.length; i++)
            {
                old[i].delete();
            }
        }

        if ("TRUE".equalsIgnoreCase(getProperty(context,
            SecurityConstants.ENABLE_PERMISSIONADMIN_PROP,
            SecurityConstants.ENABLE_PERMISSIONADMIN_VALUE)))
        {
            File cache = context.getDataFile("security" + File.separator
                + "pa.txt");
            if ((cache == null) || (!cache.isFile() && !cache.createNewFile()))
            {
                throw new IOException("Can't create cache file");
            }
            pai = new PermissionAdminImpl(permissions, new PropertiesCache(
                cache, tmp, action));
        }

        ConditionalPermissionAdminImpl cpai = null;

        if ("TRUE".equalsIgnoreCase(getProperty(context,
            SecurityConstants.ENABLE_CONDPERMADMIN_PROP,
            SecurityConstants.ENABLE_CONDPERMADMIN_VALUE)))
        {
            File cpaCache = context.getDataFile("security" + File.separator
                + "cpa.txt");
            if ((cpaCache == null)
                || (!cpaCache.isFile() && !cpaCache.createNewFile()))
            {
                throw new IOException("Can't create cache file");
            }

            LocalPermissions localPermissions = new LocalPermissions(
                permissions);

            cpai = new ConditionalPermissionAdminImpl(permissions,
                new Conditions(action), localPermissions, new PropertiesCache(
                    cpaCache, tmp, action), pai);
        }

        if ((pai != null) || (cpai != null))
        {
            String crlList = getProperty(context,
                SecurityConstants.CRL_FILE_PROP,
                SecurityConstants.CRL_FILE_VALUE);
            String storeList = getProperty(context,
                SecurityConstants.KEYSTORE_FILE_PROP,
                SecurityConstants.KEYSTORE_FILE_VALUE);
            String passwdList = getProperty(context,
                SecurityConstants.KEYSTORE_PASS_PROP,
                SecurityConstants.KEYSTORE_PASS_VALUE);
            String typeList = getProperty(context,
                SecurityConstants.KEYSTORE_TYPE_PROP,
                SecurityConstants.KEYSTORE_TYPE_VALUE);
            String osgi_keystores = getProperty(context,
                Constants.FRAMEWORK_TRUST_REPOSITORIES, null);
            if (osgi_keystores != null)
            {
                StringTokenizer tok = new StringTokenizer(osgi_keystores,
                    File.pathSeparator);

                if (storeList.length() == 0)
                {
                    storeList += "file:" + tok.nextToken();
                    passwdList += " ";
                    typeList += "JKS";
                }
                while (tok.hasMoreTokens())
                {
                    storeList += "|file:" + tok.nextToken();
                    passwdList += "| ";
                    typeList += "|JKS";
                }
            }

            StringTokenizer storeTok = new StringTokenizer(storeList, "|");
            StringTokenizer passwdTok = new StringTokenizer(passwdList, "|");
            StringTokenizer typeTok = new StringTokenizer(typeList, "|");

            if ((storeTok.countTokens() != typeTok.countTokens())
                || (passwdTok.countTokens() != storeTok.countTokens()))
            {
                throw new BundleException(
                    "Each CACerts keystore must have one type and one passwd entry and vice versa.");
            }

            SecurityProvider provider = new SecurityProviderImpl(crlList,
                typeList, passwdList, storeList, pai, cpai, action);

            ((Felix) context.getBundle(0)).setSecurityProvider(provider);
        }

        if (pai != null)
        {
            context.registerService(PermissionAdmin.class.getName(), pai, null);
        }

        if (cpai != null)
        {
            context.registerService(ConditionalPermissionAdmin.class.getName(),
                cpai, null);
        }
    }

    public synchronized void stop(BundleContext context) throws Exception
    {
        ((Felix) context.getBundle(0)).setSecurityProvider(null);
    }

    private String getProperty(BundleContext context, String key,
        String defaultValue)
    {
        String result = context.getProperty(key);

        return (result != null) ? result : defaultValue;
    }
}
