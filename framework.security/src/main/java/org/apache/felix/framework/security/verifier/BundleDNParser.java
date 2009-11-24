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
package org.apache.felix.framework.security.verifier;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.felix.framework.cache.BundleRevision;
import org.apache.felix.framework.security.util.BundleInputStream;
import org.apache.felix.framework.security.util.TrustManager;
import org.apache.felix.moduleloader.IContent;

public final class BundleDNParser
{
    private static final Method m_getCodeSigners;
    private static final Method m_getSignerCertPath;
    private static final Method m_getCertificates;

    static
    {
        Method getCodeSigners = null;
        Method getSignerCertPath = null;
        Method getCertificates = null;
        try
        {
            getCodeSigners =
                Class.forName("java.util.jar.JarEntry").getMethod(
                    "getCodeSigners", null);
            getSignerCertPath =
                Class.forName("java.security.CodeSigner").getMethod(
                    "getSignerCertPath", null);
            getCertificates =
                Class.forName("java.security.cert.CertPath").getMethod(
                    "getCertificates", null);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            getCodeSigners = null;
            getSignerCertPath = null;
            getCertificates = null;
        }
        m_getCodeSigners = getCodeSigners;
        m_getSignerCertPath = getSignerCertPath;
        m_getCertificates = getCertificates;
    }

    private final Map m_cache = new HashMap();
    private final TrustManager m_manager;

    public BundleDNParser(TrustManager manager)
    {
        m_manager = manager;
    }

    public Map getCache()
    {
        synchronized (m_cache)
        {
            return new HashMap(m_cache);
        }
    }

    public void put(String root, String[] dnChains)
    {
        synchronized (m_cache)
        {
            m_cache.put(root, dnChains);
        }
    }

    public void checkDNChains(String root, IContent content) throws Exception
    {
        synchronized (m_cache)
        {
            if (m_cache.containsKey(root))
            {
                String[] result = (String[]) m_cache.get(root);
                if ((result != null) && (result.length == 0))
                {
                    throw new IOException("Bundle not properly signed");
                }
                return;
            }
        }

        String[] result = new String[0];
        Exception org = null;
        try
        {
            result = _getDNChains(root, content);
        }
        catch (Exception ex)
        {
            org = ex;
        }

        synchronized (m_cache)
        {
            m_cache.put(root, result);
        }

        if (org != null)
        {
            throw org;
        }
    }

    public String[] getDNChains(String root, IContent bundleRevision)
    {
        synchronized (m_cache)
        {
            if (m_cache.containsKey(root))
            {
                String[] result = (String[]) m_cache.get(root);
                if ((result != null) && (result.length == 0))
                {
                    return null;
                }
                return result;
            }
        }

        String[] result = new String[0];
        
        try
        {
            result = _getDNChains(root, bundleRevision);
        }
        catch (Exception ex)
        {
            // Ignore
        }

        synchronized (m_cache)
        {
            m_cache.put(root, result);
        }

        return result;
    } 

    private String[] _getDNChains(String root, IContent content)
        throws IOException
    {
        X509Certificate[] certificates = null;

        certificates = getCertificates(new BundleInputStream(content));

        if (certificates == null)
        {
            return null;
        }

        List rootChains = new ArrayList();

        getRootChains(certificates, rootChains);

        List result = new ArrayList();

        SubjectDNParser parser = new SubjectDNParser();

        for (Iterator rootIter = rootChains.iterator(); rootIter.hasNext();)
        {
            StringBuffer buffer = new StringBuffer();

            List chain = (List) rootIter.next();

            Iterator iter = chain.iterator();

            X509Certificate current = (X509Certificate) iter.next();

            try
            {
                buffer.append(parser
                    .parseSubjectDN(current.getTBSCertificate()));

                while (iter.hasNext())
                {
                    buffer.append(';');

                    current = (X509Certificate) iter.next();

                    buffer.append(parser.parseSubjectDN(current
                        .getTBSCertificate()));
                }

                result.add(buffer.toString());

            }
            catch (Exception ex)
            {
                // something went wrong during parsing -
                // it might be that the cert contained an unsupported OID
                // TODO: log this or something
                ex.printStackTrace();
            }
        }

        if (!result.isEmpty())
        {
            return (String[]) result.toArray(new String[result.size()]);
        }

        throw new IOException();
    }

    private X509Certificate[] getCertificates(InputStream input)
        throws IOException
    {
        JarInputStream bundle = new JarInputStream(input, true);

        if (bundle.getManifest() == null)
        {
            return null;
        }

        List certificateChains = new ArrayList();

        int count = certificateChains.size();

        // This is tricky: jdk1.3 doesn't say anything about what is happening
        // if a bad sig is detected on an entry - later jdk's do say that they
        // will throw a security Exception. The below should cater for both
        // behaviors. 
        for (JarEntry entry = bundle.getNextJarEntry(); entry != null; entry =
            bundle.getNextJarEntry())
        {

            if (entry.isDirectory() || entry.getName().startsWith("META-INF"))
            {
                continue;
            }

            for (byte[] tmp = new byte[4096]; bundle.read(tmp, 0, tmp.length) != -1;)
            {
            }

            Certificate[] certificates = entry.getCertificates();

            // Workaround stupid bug in the sun jdk 1.5.x - getCertificates()
            // returns null there even if there are valid certificates.
            // This is a regression bug that has been fixed in 1.6.
            // 
            // We use reflection to see whether we have a SignerCertPath
            // for the entry (available >= 1.5) and if so check whether
            // there are valid certificates - don't try this at home.
            if ((certificates == null) && (m_getCodeSigners != null))
            {
                try
                {
                    Object[] signers =
                        (Object[]) m_getCodeSigners.invoke(entry, null);

                    if (signers != null)
                    {
                        List certChains = new ArrayList();

                        for (int i = 0; i < signers.length; i++)
                        {
                            Object path =
                                m_getSignerCertPath.invoke(signers[i], null);

                            certChains.addAll((List) m_getCertificates.invoke(
                                path, null));
                        }

                        certificates =
                            (Certificate[]) certChains
                                .toArray(new Certificate[certChains.size()]);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    // Not much we can do - probably we are not on >= 1.5
                }
            }

            if ((certificates == null) || (certificates.length == 0))
            {
                return null;
            }

            List chains = new ArrayList();

            getRootChains(certificates, chains);

            if (certificateChains.isEmpty())
            {
                certificateChains.addAll(chains);
                count = certificateChains.size();
            }
            else
            {
                for (Iterator iter2 = certificateChains.iterator(); iter2
                    .hasNext();)
                {
                    X509Certificate cert =
                        (X509Certificate) ((List) iter2.next()).get(0);
                    boolean found = false;
                    for (Iterator iter3 = chains.iterator(); iter3.hasNext();)
                    {
                        X509Certificate cert2 =
                            (X509Certificate) ((List) iter3.next()).get(0);

                        if (cert.getSubjectDN().equals(cert2.getSubjectDN())
                            && cert.equals(cert2))
                        {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        iter2.remove();
                    }
                }
            }

            if (certificateChains.isEmpty())
            {
                if (count > 0)
                {
                    throw new IOException("Bad signers");
                }
                return null;
            }
        }

        List result = new ArrayList();

        for (Iterator iter = certificateChains.iterator(); iter.hasNext();)
        {
            result.addAll((List) iter.next());
        }

        return (X509Certificate[]) result.toArray(new X509Certificate[result
            .size()]);
    }

    private boolean isRevoked(Certificate certificate)
    {
        for (Iterator iter = m_manager.getCRLs().iterator(); iter.hasNext();)
        {
            if (((CRL) iter.next()).isRevoked(certificate))
            {
                return true;
            }
        }

        return false;
    }

    private void getRootChains(Certificate[] certificates, List chains)
    {
        List chain = new ArrayList();

        boolean revoked = false;

        for (int i = 0; i < certificates.length - 1; i++)
        {
            X509Certificate certificate = (X509Certificate) certificates[i];

            if (!revoked && isRevoked(certificate))
            {
                revoked = true;
            }
            else if (!revoked)
            {
                try
                {
                    certificate.checkValidity();

                    chain.add(certificate);
                }
                catch (CertificateException ex)
                {
                    // TODO: log this or something
                    revoked = true;
                }
            }

            if (!((X509Certificate) certificates[i + 1]).getSubjectDN().equals(
                certificate.getIssuerDN()))
            {
                if (!revoked && trusted(certificate))
                {
                    chains.add(chain);
                }

                revoked = false;

                if (!chain.isEmpty())
                {
                    chain = new ArrayList();
                }
            }
        }
        // The final entry in the certs array is always
        // a "root" certificate
        if (!revoked)
        {
            chain.add(certificates[certificates.length - 1]);
            if (trusted((X509Certificate) certificates[certificates.length - 1]))
            {
                chains.add(chain);
            }
        }
    }

    private boolean trusted(X509Certificate cert)
    {
        if (m_manager.getCaCerts().isEmpty() || isRevoked(cert))
        {
            return false;
        }

        for (Iterator iter = m_manager.getCaCerts().iterator(); iter.hasNext();)
        {
            X509Certificate trustedCaCert = (X509Certificate) iter.next();

            if (isRevoked(trustedCaCert))
            {
                continue;
            }

            // If the cert has the same SubjectDN
            // as a trusted CA, check whether
            // the two certs are the same.
            if (cert.getSubjectDN().equals(trustedCaCert.getSubjectDN()))
            {
                if (cert.equals(trustedCaCert))
                {
                    try
                    {
                        cert.checkValidity();
                        trustedCaCert.checkValidity();
                        return true;
                    }
                    catch (CertificateException ex)
                    {
                        // Not much we can do
                        // TODO: log this or something
                    }
                }
            }
        }

        // cert issued by any of m_trustedCaCerts ? return true : return false
        for (Iterator iter = m_manager.getCaCerts().iterator(); iter.hasNext();)
        {
            X509Certificate trustedCaCert = (X509Certificate) iter.next();

            if (isRevoked(trustedCaCert))
            {
                continue;
            }

            if (cert.getIssuerDN().equals(trustedCaCert.getSubjectDN()))
            {
                try
                {
                    cert.verify(trustedCaCert.getPublicKey());
                    cert.checkValidity();
                    trustedCaCert.checkValidity();
                    return true;
                }
                catch (Exception ex)
                {
                    // TODO: log this or something
                }
            }
        }

        return false;
    }
}
