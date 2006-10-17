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

import java.util.*;

import org.osgi.framework.Bundle;

public final class SignerMatcher
{
    private BundleImpl m_bundleImpl = null;
    private String m_filter = null;

    public SignerMatcher(String filter)
    {
        m_filter = filter;
    }

    public SignerMatcher(Bundle bundle)
    {
        m_bundleImpl = (BundleImpl) bundle;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof SignerMatcher))
        {
            return false;
        }

        String pattern = ((SignerMatcher) o).m_filter;

        if (pattern == null)
        {
            return true;
        }

        if (m_bundleImpl == null)
        {
            return pattern.trim().equals("\\*");
        }

        String[] dns = m_bundleImpl.getSubjectDNs();

        if (dns == null)
        {
            return pattern.trim().equals("\\*");
        }

        for (int i = 0;i < dns.length;i++)
        {
            if (match(pattern, dns[i]))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean match(String pattern, String dn)
    {
        try
        {
            return ((pattern != null) && (dn != null)) ?
                matchDN(pattern.toCharArray(), 0, dn.toCharArray(), 0) : false;
        }
        catch (Exception ex)
        {
            // TODO: log this or something
            ex.printStackTrace();
        }

        return false;
    }

    private static boolean matchDN(char[] pattern, int pPos, char[] dn, int dPos)
    {
        pPos = skip(pattern, pPos, ' ');

        if (pPos >= pattern.length)
        {
            return true;
        }

        int befor = pPos;

        if ((pPos < pattern.length -1) && (pattern[pPos] == '\\') && (pattern[pPos + 1] == '*'))
        {
            pPos = pPos + 1;
        }

        switch (pattern[pPos++])
        {
            case '*':
                pPos = skip(pattern, pPos, ' ');
                if ((pPos < pattern.length) && (pattern[pPos] == ';'))
                {
                    if (matchDN(pattern, ++pPos, dn, dPos))
                    {
                        return true;
                    }
                    return matchDN(pattern, pPos, dn, skipEscapedUntil(dn, dPos, ';') + 1);
                }
                if (pPos >= pattern.length)
                {
                    return true;
                }
                return matchRDN(pattern, befor, dn, dPos);
            case '-':
                pPos = skip(pattern, pPos, ' ');
                if ((pPos < pattern.length) && (pattern[pPos] == ';'))
                {
                    int next = dPos;
                    pPos++;
                    do
                    {
                        if (matchDN(pattern, pPos, dn, next))
                        {
                            return true;
                        }
                        next = skipEscapedUntil(dn, next, ';') + 1;
                    } while (next < dn.length);

                    return false;
                }
                if (pPos >= pattern.length)
                {
                    return true;
                }
                throw new IllegalArgumentException("[" + pPos + "]" + new String(pattern));
            default:
                break;
        }

        return matchRDN(pattern, befor, dn, dPos);
    }

    private static boolean matchRDN(char[] pattern, int pPos, char[] dn, int dPos)
    {
        pPos = skip(pattern, pPos, ' ');

        if (pPos >= pattern.length)
        {
            return true;
        }

        if ((pPos < pattern.length -1) && (pattern[pPos] == '\\') && (pattern[pPos + 1] == '*'))
        {
            pPos = pPos + 1;
        }

        switch (pattern[pPos++])
        {
            case '*':
                pPos = skip(pattern, pPos, ' ');
                if ((pPos < pattern.length) && (pattern[pPos] == ','))
                {
                    pPos++;
                    do
                    {
                        if (matchKV(pattern, pPos, dn, dPos))
                        {
                            return true;
                        }

                        int comma = skipEscapedUntil(dn, dPos, ',');
                        int colon = skipEscapedUntil(dn, dPos, ';');

                        dPos = (comma > colon) ? colon : comma;
                    } while ((dPos < dn.length) && (dn[dPos++] == ','));
                    return false;
                }
                throw new IllegalArgumentException("[" + pPos + "]" + new String(pattern));
            default:
                break;
        }

        return matchKV(pattern, pPos - 1, dn, dPos);
    }

    private static boolean matchKV(char[] pattern, int pPos, char[] dn, int dPos)
    {
        pPos = skip(pattern, pPos, ' ');

        if (pPos >= pattern.length)
        {
            return false;
        }

        int equals = skipEscapedUntil(pattern, pPos, '=');
        int comma = skipEscapedUntil(pattern, pPos, ',');
        int colon = skipEscapedUntil(pattern, pPos, ';');
        if (((colon < pattern.length) && (colon < equals)) ||
            ((comma < pattern.length) && (comma < equals)) ||
            (equals >= pattern.length))
        {
            return false;
        }

        String key = (String) KEY2OIDSTRING.get(
            new String(pattern, pPos, equals - pPos).toLowerCase(Locale.US).trim());

        if (key == null)
        {
            throw new IllegalArgumentException("Bad key [" +
                new String(pattern, pPos, equals - pPos) + "] in [" +
                new String(pattern) + "]");
        }

        pPos = equals + 1;
        int keylength = key.length();
        for (int i = 0;i < keylength;i++)
        {
            if ((dPos >= dn.length) || (key.charAt(i) != dn[dPos++]))
            {
                return false;
            }
        }

        if ((dPos >= dn.length) || (dn[dPos++] != '='))
        {
            return false;
        }

        pPos = skip(pattern, pPos, ' ');
        if ((pPos < pattern.length -1) && (pattern[pPos] == '\\') && (pattern[pPos + 1] == '*'))
        {
            pPos = skip(pattern, pPos + 2, ' ');
            if (pPos >= pattern.length)
            {
                return true;
            }
            comma = skipEscapedUntil(dn, dPos, ',');
            colon = skipEscapedUntil(dn, dPos, ';');
            if ((pattern[pPos] == ',') && (colon > comma))
            {
                return matchKV(pattern, ++pPos, dn, comma + 1);
            }

            if (pattern[pPos] == ';' )
            {
                return matchDN(pattern, ++pPos, dn, colon + 1);
            }

            return false;
        }
        boolean escaped = false;
        while ((pPos < pattern.length) && (dPos < dn.length))
        {
            switch (Character.toLowerCase(pattern[pPos++]))
            {
                case ' ':
                    if ((pattern[pPos - 2] != ' ') && ((dn[dPos++] != ' ') &&
                        (dn[--dPos] != ';') && (dn[dPos] != ',')))
                    {
                        return false;
                    }
                    break;
                case '\\':
                    escaped = !escaped;
                    break;

                case '(':
                case ')':
                    if (escaped)
                    {
                        if (dn[dPos++] != pattern[pPos - 1])
                        {
                            return false;
                        }
                        escaped = false;
                        break;
                    }
                    return false;
                case ';':
                    if (!escaped)
                    {
                        if ((dPos < dn.length) && ((dn[dPos] == ',') || (dn[dPos] == ';')))
                        {
                            return matchDN(pattern, pPos, dn, skipEscapedUntil(dn, dPos, ';') + 1);
                        }
                        return false;
                    }
                case ',':
                    if (!escaped)
                    {
                        if ((dPos < dn.length) && (dn[dPos] == ','))
                        {
                            return matchKV(pattern, pPos, dn, dPos + 1);
                        }
                        return false;
                    }
                default:
                    if (escaped)
                    {
                        if (dn[dPos++] != '\\')
                        {
                            return false;
                        }
                        escaped = false;
                    }
                    if (dn[dPos++] != Character.toLowerCase(pattern[pPos - 1]))
                    {
                        return false;
                    }
                    break;
            }
        }

        pPos = skip(pattern, pPos, ' ');
        if (pPos >= pattern.length)
        {
            if ((dPos >= dn.length) || (dn[dPos] == ',') || (dn[dPos] == ';'))
            {
                return true;
            }
        }
        else
        {
            switch (pattern[pPos++])
            {
                case ',':
                    return matchKV(pattern, pPos, dn, dPos);
                case ';':
                    return matchDN(pattern, pPos, dn, dPos);
                default:
                    break;
            }
        }

        return false;
    }

    private static final Map KEY2OIDSTRING = new HashMap();

    static {
        KEY2OIDSTRING.put("2.5.4.3", "cn");
        KEY2OIDSTRING.put("cn", "cn");
        KEY2OIDSTRING.put("commonname", "cn");
        KEY2OIDSTRING.put("2.5.4.4", "sn");
        KEY2OIDSTRING.put("sn", "sn");
        KEY2OIDSTRING.put("surname", "sn");
        KEY2OIDSTRING.put("2.5.4.6", "c");
        KEY2OIDSTRING.put("c", "c");
        KEY2OIDSTRING.put("countryname", "c");
        KEY2OIDSTRING.put("2.5.4.7", "l");
        KEY2OIDSTRING.put("l", "l");
        KEY2OIDSTRING.put("localityname", "l");
        KEY2OIDSTRING.put("2.5.4.8", "st");
        KEY2OIDSTRING.put("st", "st");
        KEY2OIDSTRING.put("stateorprovincename", "st");
        KEY2OIDSTRING.put("2.5.4.10", "o");
        KEY2OIDSTRING.put("o", "o");
        KEY2OIDSTRING.put("organizationname", "o");
        KEY2OIDSTRING.put("2.5.4.11", "ou");
        KEY2OIDSTRING.put("ou", "ou");
        KEY2OIDSTRING.put("organizationalunitname", "ou");
        KEY2OIDSTRING.put("2.5.4.12", "title");
        KEY2OIDSTRING.put("t", "title");
        KEY2OIDSTRING.put("title", "title");
        KEY2OIDSTRING.put("2.5.4.42", "givenname");
        KEY2OIDSTRING.put("givenname", "givenname");
        KEY2OIDSTRING.put("2.5.4.43", "initials");
        KEY2OIDSTRING.put("initials", "initials");
        KEY2OIDSTRING.put("2.5.4.44", "generationqualifier");
        KEY2OIDSTRING.put("generationqualifier", "generationqualifier");
        KEY2OIDSTRING.put("2.5.4.46", "dnqualifier");
        KEY2OIDSTRING.put("dnqualifier", "dnqualifier");
        KEY2OIDSTRING.put("2.5.4.9", "street");
        KEY2OIDSTRING.put("street", "street");
        KEY2OIDSTRING.put("streetaddress", "street");
        KEY2OIDSTRING.put("0.9.2342.19200300.100.1.25", "dc");
        KEY2OIDSTRING.put("dc", "dc");
        KEY2OIDSTRING.put("domaincomponent", "dc");
        KEY2OIDSTRING.put("0.9.2342.19200300.100.1.1", "uid");
        KEY2OIDSTRING.put("uid", "uid");
        KEY2OIDSTRING.put("userid", "uid");
        KEY2OIDSTRING.put("1.2.840.113549.1.9.1", "emailaddress");
        KEY2OIDSTRING.put("emailaddress", "emailaddress");
        KEY2OIDSTRING.put("2.5.4.5", "serialnumber");
        KEY2OIDSTRING.put("serialnumber", "serialnumber");
    }

    private static int skipEscapedUntil(char[] string, int pos, char value)
    {
        boolean escaped = false;

        while (pos < string.length)
        {
            switch (string[pos++])
            {
                case '\\':
                    escaped = true;
                    break;
                default:
                    if (!escaped)
                    {
                        if (string[pos - 1] == value)
                        {
                            return pos - 1;
                        }
                    }
                    escaped = false;
                    break;
            }
        }

        return pos;
    }

    private static int skip(char[] string, int pos, char value)
    {
        while (pos < string.length)
        {
            if (string[pos] != value)
            {
                break;
            }
            pos++;
        }

        return pos;
    }
}
