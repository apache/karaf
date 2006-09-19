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
package org.apache.felix.framework.cache;

import java.io.File;
import java.io.InputStream;
import java.security.cert.*;
import java.util.*;
import java.util.jar.*;

import org.apache.felix.framework.Logger;
import org.apache.felix.moduleloader.IContent;

/**
 * <p>
 * This class implements an abstract revision of a bundle archive. A revision
 * is an abstraction of a bundle's actual content and is associated with a
 * parent bundle archive. A bundle archive may have multiple revisions assocaited
 * with it at one time, since updating a bundle results in a new version of the
 * bundle's content until the bundle is refreshed. Upon a refresh, then old
 * revisions are then purged. This abstract class is the base class for all
 * concrete types of revisions, such as ones for a JAR file or directories. All
 * revisions are assigned a root directory into which all of their state should
 * be stored, if necessary. Clean up of this directory is the responsibility
 * of the parent bundle archive and not of the revision itself.
 * </p>
 * @see org.apache.felix.framework.cache.BundleCache
 * @see org.apache.felix.framework.cache.BundleArchive
**/
public abstract class BundleRevision
{
    private Logger m_logger;
    private File m_revisionRootDir = null;
    private String m_location = null;
    private Collection m_trustedCaCerts = null;
    private X509Certificate[] m_certificates = null;
    private String[] m_subjectDNChain = null;
    private boolean m_certInitDone = (System.getSecurityManager() == null);
    private boolean m_subjectDNInitDone = (System.getSecurityManager() == null);

    /**
     * <p>
     * This constructor is only used by the system bundle archive.
     * </p>
    **/
    BundleRevision()
    {
    }

    /**
     * <p>
     * This class is abstract and cannot be created. It represents a revision
     * of a bundle, i.e., its content. A revision is associated with a particular
     * location string, which is typically in URL format. Subclasses of this
     * class provide particular functionality, such as a revision in the form
     * of a JAR file or a directory. Each revision subclass is expected to use
     * the root directory associated with the abstract revision instance to
     * store any state; this will ensure that resources used by the revision are
     * properly freed when the revision is no longer needed.
     * </p>
     * @param logger a logger for use by the revision.
     * @param revisionRootDir the root directory to be used by the revision
     *        subclass for storing any state.
     * @param location the location string associated with the revision.
     * @param trustedCaCerts the trusted CA certificates if any.
     * @throws Exception if any errors occur.
    **/
    public BundleRevision(Logger logger, File revisionRootDir, String location)
        throws Exception
    {
        m_logger = logger;
        m_revisionRootDir = revisionRootDir;
        m_location = location;
    }


    /**
     * <p>
     * Returns the logger for this revision.
     * <p>
     * @return the logger instance for this revision.
    **/
    public Logger getLogger()
    {
        return m_logger;
    }

    /**
     * <p>
     * Returns the root directory for this revision.
     * </p>
     * @return the root directory for this revision.
    **/
    public File getRevisionRootDir()
    {
        return m_revisionRootDir;
    }

    /**
     * <p>
     * Returns the location string this revision.
     * </p>
     * @return the location string for this revision.
    **/
    public String getLocation()
    {
        return m_location;
    }

    /**
     * <p>
     * Returns the main attributes of the JAR file manifest header of the
     * revision. The returned map is case insensitive.
     * </p>
     * @return the case-insensitive JAR file manifest header of the revision.
     * @throws java.lang.Exception if any error occurs.
    **/
    public abstract Map getManifestHeader() throws Exception;

    /**
     * <p>
     * Returns a content object that is associated with the revision.
     * </p>
     * @return a content object that is associated with the revision.
     * @throws java.lang.Exception if any error occurs.
    **/
    public abstract IContent getContent() throws Exception;

    /**
     * <p>
     * Returns an array of content objects that are associated with the
     * specified revision's bundle class path.
     * </p>
     * @return an array of content objects for the revision's bundle class path.
     * @throws java.lang.Exception if any error occurs.
    **/
    public abstract IContent[] getContentPath() throws Exception;

    /**
     * <p>
     * Returns the absolute file path for the specified native library of the
     * revision.
     * </p>
     * @param libName the name of the library.
     * @return a <tt>String</tt> that contains the absolute path name to
     *         the requested native library of the revision.
     * @throws java.lang.Exception if any error occurs.
    **/
    public abstract String findLibrary(String libName) throws Exception;

    /**
     * <p>
     * This method is called when the revision is no longer needed. The directory
     * associated with the revision will automatically be removed for each
     * revision, so this method only needs to be concerned with other issues,
     * such as open files.
     * </p>
     * @throws Exception if any error occurs.
    **/
    public abstract void dispose() throws Exception;

    protected void setTrustedCaCerts(Collection trustedCaCerts)
    {
        m_trustedCaCerts = trustedCaCerts;
    }

    public X509Certificate[] getCertificates()
    {
        if (m_certInitDone)
        {
            return m_certificates;
        }

        if (m_trustedCaCerts == null)
        {
            return null;
        }

        try
        {
            m_certificates = getRevisionCertificates();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            // TODO: log this or something
        }
        finally
        {
            m_certInitDone = true;
        }

        return m_certificates;
    }

    protected abstract X509Certificate[] getRevisionCertificates() throws Exception;

    public String[] getDNChains()
    {
        if (m_subjectDNInitDone)
        {
            return m_subjectDNChain;
        }

        try
        {
            X509Certificate[] certificates = getCertificates();

            if (certificates == null)
            {
                return null;
            }

            List rootChains = new ArrayList();

            getRootChains(certificates, rootChains);

            List result = new ArrayList();

            for (Iterator rootIter = rootChains.iterator();rootIter.hasNext();)
            {
                StringBuffer buffer = new StringBuffer();

                List chain = (List) rootIter.next();

                Iterator iter = chain.iterator();

                X509Certificate current = (X509Certificate) iter.next();

                try
                {
                    buffer.append(parseSubjectDN(current.getTBSCertificate()));

                    while (iter.hasNext())
                    {
                        buffer.append(';');

                        current = (X509Certificate) iter.next();

                        buffer.append(parseSubjectDN(current.getTBSCertificate()));
                    }

                    result.add(buffer.toString());

                }
                catch (Exception ex)
                {
                    // something went wrong during parsing -
                    // it might be that the cert contained an unsupported OID
                    ex.printStackTrace();
                    // TODO: log this or something
                }
            }

            if (!result.isEmpty())
            {
                m_subjectDNChain = (String[]) result.toArray(new String[result.size()]);
            }
        }
        finally
        {
            m_subjectDNInitDone = true;
        }

        return m_subjectDNChain;
    }

    protected X509Certificate[] getCertificatesForJar(JarFile bundle)
        throws Exception
    {
        if (bundle.getManifest() == null)
        {
           return null;
        }

        List bundleEntries = new ArrayList();

        Enumeration entries = bundle.entries();

        while (entries.hasMoreElements())
        {
            JarEntry entry = (JarEntry) entries.nextElement();
            bundleEntries.add(entry);
            InputStream is = bundle.getInputStream(entry);
            byte[] read = new byte[4096];
            while (is.read(read) != -1)
            {
                // read the entry
            }
            is.close();
        }
        bundle.close();

        List certificateChains = new ArrayList();

        for (Iterator iter = bundleEntries.iterator();iter.hasNext();)
        {
            JarEntry entry = (JarEntry) iter.next();

            if (entry.isDirectory() || entry.getName().startsWith("META-INF"))
            {
                continue;
            }

            Certificate[] certificates = entry.getCertificates();

            if ((certificates == null) || (certificates.length == 0))
            {
                return null;
            }

            List chains = new ArrayList();

            getRootChains(certificates, chains);

            if (certificateChains.isEmpty())
            {
                certificateChains.addAll(chains);
            }
            else
            {
                for (Iterator iter2 = certificateChains.iterator();iter2.hasNext();)
                {
                    X509Certificate cert = (X509Certificate) ((List) iter2.next()).get(0);
                    boolean found = false;
                    for (Iterator iter3 = chains.iterator();iter3.hasNext();)
                    {
                        X509Certificate cert2 = (X509Certificate) ((List) iter3.next()).get(0);

                        if (cert.getSubjectDN().equals(cert2.getSubjectDN()) && cert.equals(cert2))
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
                return null;
            }
        }

        List result = new ArrayList();

        for (Iterator iter = certificateChains.iterator();iter.hasNext();)
        {
            result.addAll((List) iter.next());
        }

        return (X509Certificate[]) result.toArray(new X509Certificate[result.size()]);
    }

    protected void getRootChains(Certificate[] certificates, List chains)
    {
        List chain = new ArrayList();

        for (int i = 0; i < certificates.length - 1; i++)
        {
            chain.add(certificates[i]);
            if (!((X509Certificate)certificates[i + 1]).getSubjectDN().equals(
                ((X509Certificate)certificates[i]).getIssuerDN()))
            {

                if (trusted((X509Certificate) certificates[i]))
                {
                    chains.add(chain);
                }

                chain = new ArrayList();
            }
        }
        // The final entry in the certs array is always
        // a "root" certificate
        chain.add(certificates[certificates.length - 1]);

        if (trusted((X509Certificate) certificates[certificates.length - 1]))
        {
            chains.add(chain);
        }
    }

    // @return true if
    // m_trustedCaCerts.contains(cert) || cert issued by any of m_trustedCaCerts
    protected boolean trusted(X509Certificate cert)
    {
        if (m_trustedCaCerts == null)
        {
            return false;
        }

        // m_trustedCaCerts.contains(cert) ? return true
        for (Iterator iter = m_trustedCaCerts.iterator();iter.hasNext();)
        {
            X509Certificate trustedCaCert = (X509Certificate) iter.next();

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
                        System.err.println("WARNING: Invalid certificate [" + ex + "]");
                    }
                }
            }
        }

        // cert issued by any of m_trustedCaCerts ? return true : return false
        for (Iterator iter = m_trustedCaCerts.iterator();iter.hasNext();)
        {
            X509Certificate trustedCaCert = (X509Certificate) iter.next();

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
                    System.err.println("WARNING: Invalid certificate [" + ex + "]");
                }
            }
        }

        return false;
    }

    /*
     * This is deep magiK, bare with me. The problem is that we don't get
     * access to the original subject dn in a certificate without resorting to
     * sun.* classes or running on something > OSGi-minimum/jdk1.3. Furthermore,
     * we need access to it because there is no other way to escape it properly.
     * Note, this is due to missing of a public X500Name in OSGI-minimum/jdk1.3
     * a.k.a foundation.
     *
     * The solution is to get the DER encoded TBS certificate bytes via the
     * available java methods and parse-out the subject dn in canonical form by
     * hand. This is possible without deploying a full-blown BER encoder/decoder
     * due to java already having done all the cumbersome verification and
     * normalization work.
     *
     * The following skips through the TBS certificate bytes until it reaches and
     * subsequently parses the subject dn. If the below makes immediate sense to
     * you - you either are a X509/X501/DER expert or quite possibly mad. In any
     * case, please seek medical care immediately.
     */
    protected String parseSubjectDN(byte[] tbsCertEncoded) throws Exception
    {
        // init
        tbs_buffer = tbsCertEncoded;
        tbs_offset = 0;

        try // this is a finally block that resets the tbs_buffer to null after we're done
        {
            // TBSCertificate  ::=  SEQUENCE  {
            //    version         [0]  EXPLICIT Version DEFAULT v1,
            //    serialNumber         CertificateSerialNumber,
            //    signature            AlgorithmIdentifier,
            //    issuer               Name,
            //    validity             Validity,
            //    subject              Name,
            //
            // WE CAN STOP!
            //
            //    subjectPublicKeyInfo SubjectPublicKeyInfo,
            //    issuerUniqueID  [1]  IMPLICIT UniqueIdentifier OPTIONAL,
            //                         -- If present, version must be v2 or v3
            //    subjectUniqueID [2]  IMPLICIT UniqueIdentifier OPTIONAL,
            //                         -- If present, version must be v2 or v3
            //    extensions      [3]  EXPLICIT Extensions OPTIONAL
            //                         -- If present, version must be v3
            //    }

            next();
            next();
            // if a version is present skip it
            if (tbs_tag == 0)
            {
                next();
                tbs_offset += tbs_length;
            }
            tbs_offset += tbs_length;
            // skip the serialNumber
            next();
            next();
            tbs_offset += tbs_length;
            // skip the signature
            next();
            tbs_offset += tbs_length;
            // skip the issuer
            // The issuer is a sequence of sets of issuer dns like the subject later on -
            // we just skip it.
            next();
            int endOffset = tbs_offset + tbs_length;

            int seqTagOffset = tbs_tagOffset;

            // skip the sequence
            while (endOffset > tbs_offset)
            {
                next();

                int endOffset2 = tbs_offset + tbs_length;

                int seqTagOffset2 = tbs_tagOffset;

                // skip each set
                while (endOffset2 > tbs_offset)
                {
                    next();
                    next();
                    tbs_offset += tbs_length;
                    next();
                    tbs_offset += tbs_length;
                }

                tbs_tagOffset = seqTagOffset2;
            }

            tbs_tagOffset = seqTagOffset;
            // skip the validity which contains two dates to be skiped
            next();
            next();
            tbs_offset += tbs_length;
            next();
            tbs_offset += tbs_length;
            next();
            // Now extract the subject dns and add them to attributes
            List attributes = new ArrayList();

            endOffset = tbs_offset + tbs_length;

            seqTagOffset = tbs_tagOffset;

            // for each set of rdns
            while (endOffset > tbs_offset)
            {
                next();
                int endOffset2 = tbs_offset + tbs_length;

                // store tag offset
                int seqTagOffset2 = tbs_tagOffset;

                List rdn = new ArrayList();

                // for each rdn in the set
                while (endOffset2 > tbs_offset)
                {
                    next();
                    next();
                    tbs_offset += tbs_length;
                    // parse the oid of the rdn
                    int oidElement = 1;
                    for (int i = 0; i < tbs_length; i++, ++oidElement)
                    {
                        while ((tbs_buffer[tbs_contentOffset + i] & 0x80) == 0x80)
                        {
                            i++;
                        }
                    }
                    int[] oid = new int[oidElement];
                    for (int id = 1, i = 0; id < oid.length; id++, i++)
                    {
                        int octet = tbs_buffer[tbs_contentOffset + i];
                        oidElement = octet & 0x7F;
                        while ((octet & 0x80) != 0)
                        {
                            i++;
                            octet = tbs_buffer[tbs_contentOffset + i];
                            oidElement = oidElement << 7 | (octet & 0x7f);
                        }
                        oid[id] = oidElement;
                    }
                    // The first OID is special
                    if (oid[1] > 79)
                    {
                        oid[0] = 2;
                        oid[1] = oid[1] - 80;
                    }
                    else
                    {
                        oid[0] = oid[1] / 40;
                        oid[1] = oid[1] % 40;
                    }
                    // Now parse the value of the rdn
                    next();
                    String str = null;
                    int tagTmp = tbs_tag;
                    tbs_offset += tbs_length;
                    switch(tagTmp)
                    {
                        case 30: // BMPSTRING
                        case 22: // IA5STRING
                        case 27: // GENERALSTRING
                        case 19: // PRINTABLESTRING
                        case 20: // TELETEXSTRING && T61STRING
                        case 28: // UNIVERSALSTRING
                            str = new String(tbs_buffer, tbs_contentOffset,
                                tbs_length);
                            break;
                        case 12: // UTF8_STRING
                            str = new String(tbs_buffer, tbs_contentOffset,
                                tbs_length, "UTF-8");
                            break;
                        default: // OCTET
                            byte[] encoded = new byte[tbs_offset - tbs_tagOffset];
                            System.arraycopy(tbs_buffer, tbs_tagOffset, encoded,
                                0, encoded.length);
                            // Note, I'm not sure this is allowed by the spec
                            // i.e., whether OCTET subjects are allowed at all
                            // but it shouldn't harm doing it anyways (we just
                            // convert it into a hex string prefixed with \#).
                            str = toHexString(encoded);
                            break;
                    }

                    rdn.add(new Object[]{mapOID(oid), makeCanonical(str)});
                }

                attributes.add(rdn);
                tbs_tagOffset = seqTagOffset2;
            }

            tbs_tagOffset = seqTagOffset;

            StringBuffer result = new StringBuffer();

            for (int i = attributes.size() - 1; i >= 0; i--)
            {
                List rdn = (List) attributes.get(i);
                Collections.sort(rdn, new Comparator()
                {
                    public int compare(Object obj1, Object obj2)
                    {
                        return ((String) ((Object[]) obj1)[0]).compareTo(
                            ((String) ((Object[])obj2)[0]));
                    }
                });

                for (Iterator iter = rdn.iterator();iter.hasNext();)
                {
                    Object[] att = (Object[]) iter.next();
                    result.append((String) att[0]);
                    result.append('=');
                    result.append((String) att[1]);

                    if (iter.hasNext())
                    {
                        // multi-valued RDN
                        result.append('+');
                    }
                }

                if (i != 0)
                {
                    result.append(',');
                }
            }

            // the spec says:
            // return result.toString().toUpperCase(Locale.US).toLowerCase(Locale.US);
            // but that doesn't make no sense to me whatsoever hence,
            return result.toString().toLowerCase(Locale.US);
        }
        finally
        {
            tbs_buffer = null;
        }
    }

    private byte[] tbs_buffer = null;
    private int tbs_offset  = 0;
    private int tbs_tagOffset = 0;
    private int tbs_tag = -1;
    private int tbs_length = -1;
    private int tbs_contentOffset = -1;

    // Determine the type of the current sequence (tbs_tab), and the length and
    // offset of it (tbs_length and tbs_tagOffset) plus increment the global
    // offset (tbs_offset) accordingly. Note, we don't need to check for
    // the indefinite length because this is supposed to be DER not BER (and
    // we implicitly assume that java only gives us valid DER).
    private void next()
    {
        tbs_tagOffset = tbs_offset;
        tbs_tag = tbs_buffer[tbs_offset++] & 0xFF;
        tbs_length = tbs_buffer[tbs_offset++] & 0xFF;
        // There are two kinds of length forms - make sure we use the right one
        if ((tbs_length & 0x80) != 0)
        {
            // its the long kind
            int numOctets = tbs_length & 0x7F;
            // hence, convert it
            tbs_length = tbs_buffer[tbs_offset++] & 0xFF;
            for (int i = 1; i < numOctets; i++)
            {
                int ch = tbs_buffer[tbs_offset++] & 0xFF;
                tbs_length = (tbs_length << 8) + ch;
            }
        }
        tbs_contentOffset = tbs_offset;
    }

    private String makeCanonical(String value)
    {
        int len = value.length();

        if (len == 0)
        {
            return value;
        }

        StringBuffer result = new StringBuffer(len);

        int i = 0;
        if (value.charAt(0) == '#')
        {
            result.append('\\');
            result.append('#');
            i++;
        }
        for (;i < len; i++)
        {
            char c = value.charAt(i);

            switch (c)
            {
                case ' ':
                    int pos = result.length();
                    // remove leading spaces and
                    // remove all spaces except one in any sequence of spaces
                    if ((pos == 0) || (result.charAt(pos - 1) == ' '))
                    {
                        break;
                    }
                    result.append(' ');
                    break;
                case '"':
                case '\\':
                case ',':
                case '+':
                case '<':
                case '>':
                case ';':
                    result.append('\\');
                default:
                    result.append(c);
            }
        }

        // count down until first none space to remove trailing spaces
        i = result.length() - 1;
        while ((i > -1) && (result.charAt(i) == ' '))
        {
            i--;
        }

        result.setLength(i + 1);

        return result.toString();
    }

    private String toHexString(byte[] encoded)
    {
        StringBuffer result = new StringBuffer();

        result.append('#');

        for (int i = 0; i < encoded.length; i++)
        {
            int c = (encoded[i] >> 4) & 0x0F;
            if (c < 10)
            {
                result.append((char) (c + 48));
            }
            else
            {
                result.append((char) (c + 87));
            }

            c = encoded[i] & 0x0F;

            if (c < 10)
            {
                result.append((char) (c + 48));
            }
            else
            {
                result.append((char) (c + 87));
            }
        }

        return result.toString();
    }

    private static final Map OID2NAME = new HashMap();

    static
    {
        // see core-spec 2.3.5
        OID2NAME.put("2.5.4.3", "cn");
        OID2NAME.put("2.5.4.4", "sn");
        OID2NAME.put("2.5.4.6", "c");
        OID2NAME.put("2.5.4.7", "l");
        OID2NAME.put("2.5.4.8", "st");
        OID2NAME.put("2.5.4.10", "o");
        OID2NAME.put("2.5.4.11", "ou");
        OID2NAME.put("2.5.4.12", "title");
        OID2NAME.put("2.5.4.42", "givenname");
        OID2NAME.put("2.5.4.43", "initials");
        OID2NAME.put("2.5.4.44", "generationqualifier");
        OID2NAME.put("2.5.4.46", "dnqualifier");
        OID2NAME.put("2.5.4.9", "street");
        OID2NAME.put("0.9.2342.19200300.100.1.25", "dc");
        OID2NAME.put("0.9.2342.19200300.100.1.1", "uid");
        OID2NAME.put("1.2.840.113549.1.9.1", "emailaddress");
        OID2NAME.put("2.5.4.5", "serialnumber");
        // p.s.: it sucks that the spec doesn't list some of the oids used
        // p.p.s: it sucks that the spec doesn't list the short form for all names
        // In summary, there is a certain amount of guess-work involved but I'm
        // fairly certain I've got it right.
    }

    // This just creates a string of the oid and looks for its name in the
    // known names map OID2NAME. There might be faster implementations :-)
    private String mapOID(int[] oid)
    {
        StringBuffer oidString = new StringBuffer();

        oidString.append(oid[0]);
        for (int i = 1;i < oid.length;i++)
        {
            oidString.append('.');
            oidString.append(oid[i]);
        }

        String result = (String) OID2NAME.get(oidString.toString());

        if (result == null)
        {
            throw new IllegalArgumentException("Unknown oid: " + oidString.toString());
        }

        return result;
    }
}