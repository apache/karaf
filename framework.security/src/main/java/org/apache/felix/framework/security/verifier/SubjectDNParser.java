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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SubjectDNParser
{
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
    
    private byte[] m_buffer;
    private int m_offset  = 0;
    private int m_tagOffset = 0;
    private int m_tag = -1;
    private int m_length = -1;
    private int m_contentOffset = -1;
    
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
    public String parseSubjectDN(byte[] tbsBuffer) throws Exception
    {
        // init
        m_buffer = tbsBuffer;
        m_offset = 0;

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
        try 
        {
            next();
            next();
            // if a version is present skip it
            if (m_tag == 0)
            {
                next();
                m_offset += m_length;
            }
            m_offset += m_length;
            // skip the serialNumber
            next();
            next();
            m_offset += m_length;
            // skip the signature
            next();
            m_offset += m_length;
            // skip the issuer
            // The issuer is a sequence of sets of issuer dns like the subject later on -
            // we just skip it.
            next();
            int endOffset = m_offset + m_length;
    
            int seqTagOffset = m_tagOffset;
    
            // skip the sequence
            while (endOffset > m_offset)
            {
                next();
    
                int endOffset2 = m_offset + m_length;
    
                int seqTagOffset2 = m_tagOffset;
    
                // skip each set
                while (endOffset2 > m_offset)
                {
                    next();
                    next();
                    m_offset += m_length;
                    next();
                    m_offset += m_length;
                }
    
                m_tagOffset = seqTagOffset2;
            }
    
            m_tagOffset = seqTagOffset;
            // skip the validity which contains two dates to be skiped
            next();
            next();
            m_offset += m_length;
            next();
            m_offset += m_length;
            next();
            // Now extract the subject dns and add them to attributes
            List attributes = new ArrayList();
    
            endOffset = m_offset + m_length;
    
            seqTagOffset = m_tagOffset;
    
            // for each set of rdns
            while (endOffset > m_offset)
            {
                next();
                int endOffset2 = m_offset + m_length;
    
                // store tag offset
                int seqTagOffset2 = m_tagOffset;
    
                List rdn = new ArrayList();
    
                // for each rdn in the set
                while (endOffset2 > m_offset)
                {
                    next();
                    next();
                    m_offset += m_length;
                    // parse the oid of the rdn
                    int oidElement = 1;
                    for (int i = 0; i < m_length; i++, ++oidElement)
                    {
                        while ((m_buffer[m_contentOffset + i] & 0x80) == 0x80)
                        {
                            i++;
                        }
                    }
                    int[] oid = new int[oidElement];
                    for (int id = 1, i = 0; id < oid.length; id++, i++)
                    {
                        int octet = m_buffer[m_contentOffset + i];
                        oidElement = octet & 0x7F;
                        while ((octet & 0x80) != 0)
                        {
                            i++;
                            octet = m_buffer[m_contentOffset + i];
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
                    int tagTmp = m_tag;
                    m_offset += m_length;
                    switch(tagTmp)
                    {
                        case 30: // BMPSTRING
                        case 22: // IA5STRING
                        case 27: // GENERALSTRING
                        case 19: // PRINTABLESTRING
                        case 20: // TELETEXSTRING && T61STRING
                        case 28: // UNIVERSALSTRING
                            str = new String(m_buffer, m_contentOffset,
                                m_length);
                            break;
                        case 12: // UTF8_STRING
                            str = new String(m_buffer, m_contentOffset,
                                m_length, "UTF-8");
                            break;
                        default: // OCTET
                            byte[] encoded = new byte[m_offset - m_tagOffset];
                            System.arraycopy(m_buffer, m_tagOffset, encoded,
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
                m_tagOffset = seqTagOffset2;
            }
    
            m_tagOffset = seqTagOffset;
    
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
            // which is needed because toLowerCase can be ambiguous in unicode when
            // used on mixed case while toUpperCase not hence, this way its ok.
            return result.toString().toUpperCase(Locale.US).toLowerCase(Locale.US);
        }
        finally
        {
            m_buffer = null;
        }
    }

    // Determine the type of the current sequence (tbs_tab), and the length and
    // offset of it (tbs_length and tbs_tagOffset) plus increment the global
    // offset (tbs_offset) accordingly. Note, we don't need to check for
    // the indefinite length because this is supposed to be DER not BER (and
    // we implicitly assume that java only gives us valid DER).
    private void next()
    {
        m_tagOffset = m_offset;
        m_tag = m_buffer[m_offset++] & 0xFF;
        m_length = m_buffer[m_offset++] & 0xFF;
        // There are two kinds of length forms - make sure we use the right one
        if ((m_length & 0x80) != 0)
        {
            // its the long kind
            int numOctets = m_length & 0x7F;
            // hence, convert it
            m_length = m_buffer[m_offset++] & 0xFF;
            for (int i = 1; i < numOctets; i++)
            {
                int ch = m_buffer[m_offset++] & 0xFF;
                m_length = (m_length << 8) + ch;
            }
        }
        m_contentOffset = m_offset;
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