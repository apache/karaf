/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/BundleSignerCondition.java,v 1.10 2006/06/16 16:31:37 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2005, 2006). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.condpermadmin;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Condition to test if the signer of a bundle matches a pattern. Since the
 * bundle's signer can only change when the bundle is updated, this condition is
 * immutable.
 * <p>
 * The condition expressed using a single String that specifies a Distinguished
 * Name (DN) chain to match bundle signers against. DN's are encoded using IETF
 * RFC 2253. Usually signers use certificates that are issued by certificate
 * authorities, which also have a corresponding DN and certificate. The
 * certificate authorities can form a chain of trust where the last DN and
 * certificate is known by the framework. The signer of a bundle is expressed as
 * signers DN followed by the DN of its issuer followed by the DN of the next
 * issuer until the DN of the root certificate authority. Each DN is separated
 * by a semicolon.
 * <p>
 * A bundle can satisfy this condition if one of its signers has a DN chain that
 * matches the DN chain used to construct this condition. Wildcards (`*') can be
 * used to allow greater flexibility in specifying the DN chains. Wildcards can
 * be used in place of DNs, RDNs, or the value in an RDN. If a wildcard is used
 * for a value of an RDN, the value must be exactly "*" and will match any value
 * for the corresponding type in that RDN. If a wildcard is used for a RDN, it
 * must be the first RDN and will match any number of RDNs (including zero
 * RDNs).
 * 
 * @version $Revision: 1.10 $
 */
/*
 * TODO: In our case the above is not correct. We don't make this an immutable
 * condition because the spec is somewhat ambiguous in regard to when the
 * signature change. This probably has to be clarified and then revisited later.
 */
public class BundleSignerCondition
{
    /*
     * NOTE: A framework implementor may also choose to replace this class in
     * their distribution with a class that directly interfaces with the
     * framework implementation. This replacement class MUST NOT alter the
     * public/protected signature of this class.
     */

    private static final String CONDITION_TYPE =
        "org.osgi.service.condpermadmin.BundleSignerCondition";

    /**
     * Constructs a Condition that tries to match the passed Bundle's location
     * to the location pattern.
     * 
     * @param bundle
     *                The Bundle being evaluated.
     * @param info
     *                The ConditionInfo to construct the condition for. The args
     *                of the ConditionInfo specify a single String specifying
     *                the chain of distinguished names pattern to match against
     *                the signer of the Bundle.
     * @return A Condition which checks the signers of the specified bundle.
     */
    static public Condition getCondition(Bundle bundle, ConditionInfo info)
    {
        if (!CONDITION_TYPE.equals(info.getType()))
            throw new IllegalArgumentException(
                "ConditionInfo must be of type \"" + CONDITION_TYPE + "\"");
        final String[] args = info.getArgs();
        if (args.length != 1)
            throw new IllegalArgumentException("Illegal number of args: "
                + args.length);

        return new ConditionImpl(bundle, "(signer=" + escapeFilter(args[0])
            + ")");

    }

    private static String escapeFilter(String string)
    {
        boolean escaped = false;
        int inlen = string.length();
        int outlen = inlen << 1; /* inlen * 2 */

        char[] output = new char[outlen];
        string.getChars(0, inlen, output, inlen);

        int cursor = 0;
        for (int i = inlen; i < outlen; i++)
        {
            char c = output[i];
            switch (c)
            {
                case '\\':
                case '(':
                case ')':
                case '*':
                    output[cursor] = '\\';
                    cursor++;
                    escaped = true;
                    break;
            }

            output[cursor] = c;
            cursor++;
        }

        return escaped ? new String(output, 0, cursor) : string;
    }

    private BundleSignerCondition()
    {
        // private constructor to prevent objects of this type
    }
}

final class ConditionImpl implements Condition, PrivilegedExceptionAction
{
    private static final Method m_getSignerMatcher;

    static
    {
        m_getSignerMatcher =
            (Method) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    Method getSignerMatcher = null;
                    try
                    {
                        getSignerMatcher =
                            Class.forName(
                                "org.apache.felix.framework.BundleImpl")
                                .getDeclaredMethod("getSignerMatcher", null);
                        getSignerMatcher.setAccessible(true);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        getSignerMatcher = null;
                    }
                    return getSignerMatcher;
                }
            });
    }

    private final Bundle m_bundle;
    private final Filter m_filter;
    private final Dictionary m_dict;

    ConditionImpl(Bundle bundle, String filter)
    {
        m_bundle = bundle;
        try
        {
            m_filter = FrameworkUtil.createFilter(filter);
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        try
        {
            Object signerMatcher = AccessController.doPrivileged(this);
            m_dict = new Hashtable();
            m_dict.put("signer", signerMatcher);
        }
        catch (PrivilegedActionException e)
        {
            if (e.getException() instanceof RuntimeException)
            {
                throw (RuntimeException) e.getException();
            }

            throw new RuntimeException(e.getException().getMessage());
        }
    }

    public boolean isMutable()
    {
        return true;
    }

    public boolean isPostponed()
    {
        return false;
    }

    public Object run() throws Exception
    {
        return m_getSignerMatcher.invoke(m_bundle, null);
    }

    public boolean isSatisfied()
    {
        return m_filter.match(m_dict);
    }

    public boolean isSatisfied(Condition[] conditions, Dictionary context)
    {
        return false;
    }
}