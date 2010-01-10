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

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.felix.framework.util.SecureAction;

/*
 * TODO: the certificate stores as well as the CRLs might change over time 
 * (added/removed certificates). We need a way to detect that and act on it. 
 * The problem is to find a good balance between re-checking and caching...
 */
public final class TrustManager
{
    private final SecureAction m_action;
    private final String m_crlList;
    private final String m_typeList;
    private final String m_passwdList;
    private final String m_storeList;
    private Collection m_caCerts = null;
    private Collection m_crls = null;

    public TrustManager(String crlList, String typeList, String passwdList,
        String storeList, SecureAction action)
    {
        m_crlList = crlList;
        m_typeList = typeList;
        m_passwdList = passwdList;
        m_storeList = storeList;
        m_action = action;
    }

    private synchronized void init()
    {
        if (m_caCerts == null)
        {
            try
            {
                initCRLs();
                initCaCerts();
            }
            catch (Exception ex)
            {
                m_caCerts = new ArrayList();
                m_crls = new ArrayList();
                // TODO: log this
                ex.printStackTrace();
            }
        }
    }

    private void initCRLs() throws Exception
    {
        final Collection result = new ArrayList();

        if (m_crlList.trim().length() != 0)
        {
            CertificateFactory fac = CertificateFactory.getInstance("X509");

            for (StringTokenizer tok = new StringTokenizer(m_crlList, "|"); tok
                .hasMoreElements();)
            {
                InputStream input = null;
                try
                {
                    input = m_action.getURLConnectionInputStream(m_action
                        .createURL(null, tok.nextToken(), null)
                        .openConnection());
                    result.addAll(fac.generateCRLs(input));
                }
                catch (Exception ex)
                {
                    // TODO: log this or something
                    ex.printStackTrace();
                }
                finally
                {
                    if (input != null)
                    {
                        try
                        {
                            input.close();
                        }
                        catch (Exception ex)
                        {
                            // TODO: log this or something
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        m_crls = result;
    }

    private void initCaCerts() throws Exception
    {
        final Collection result = new ArrayList();

        if (m_storeList.trim().length() != 0)
        {

            StringTokenizer storeTok = new StringTokenizer(m_storeList, "|");
            StringTokenizer passwdTok = new StringTokenizer(m_passwdList, "|");
            StringTokenizer typeTok = new StringTokenizer(m_typeList, "|");

            while (storeTok.hasMoreTokens())
            {
                KeyStore ks = KeyStore.getInstance(typeTok.nextToken().trim());

                InputStream input = null;
                try
                {
                    input = m_action.getURLConnectionInputStream(m_action
                        .createURL(null, storeTok.nextToken().trim(), null)
                        .openConnection());
                    String pass = passwdTok.nextToken().trim();

                    ks.load(input, (pass.length() > 0) ? pass.toCharArray()
                        : null);

                    for (Enumeration e = ks.aliases(); e.hasMoreElements();)
                    {
                        String alias = (String) e.nextElement();
                        result.add(ks.getCertificate(alias));
                    }
                }
                catch (Exception ex)
                {
                    // TODO: log this or something
                    ex.printStackTrace();
                }
                finally
                {
                    if (input != null)
                    {
                        try
                        {
                            input.close();
                        }
                        catch (Exception ex)
                        {
                            // TODO: log this or something
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        m_caCerts = result;
    }

    public Collection getCRLs()
    {
        init();

        return m_crls;
    }

    public Collection getCaCerts()
    {
        init();

        return m_caCerts;
    }
}