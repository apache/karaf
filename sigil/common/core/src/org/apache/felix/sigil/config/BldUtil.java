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

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// taken from Newton Launcher

public class BldUtil
{
    /**
     * expands property references embedded in strings. Each occurrence of ${name} is replaced with the value of
     * p.getProperty("name"); If the property is not set, then the original reference, is returned as follows "?<name>".
     * 
     * Strings to be expanded should not contain $ or }, except to indicate expansions.
     * 
     * Value is expanded recursively and so can contain further ${name} references. Also supports shell-expansions
     * ${name:-value}, ${name:=value} and ${name:+value}.
     * 
     * <pre>
     *      ${parameter}
     *      The value of parameter is substituted.
     *      ${parameter:-word}
     *      Use  Default  Values.  If parameter is null, the expansion of word
     *      is substituted.  Otherwise, the value of  parameter is substituted.
     *      ${parameter:=word}
     *      Assign  Default  Values.   If  parameter is null, the expansion of
     *      word is assigned to parameter.  The value of parameter  is  then
     *      substituted.
     *      ${parameter:+word}
     *      Use Alternate Value.  If parameter is null, nothing  is
     *      substituted, otherwise the expansion of word is substituted.
     *      ${parameter:?word}
     *      Raise Error.  If parameter is null, a RuntimeException is thown,
     *      with word as the message.
     * </pre>
     */
    public static String expand(String s, Properties p)
    {
        // regex to match property references e.g. ${name}
        // TODO this is very simplistic, so strings to be expanded should not
        // contain $ or }, except where substitution is expected.
        // Update: propRef regex now allows substitutions to contain $,
        // e.g. where a Windows ${user.name} is $Admin or similar.
        final Pattern propRef = Pattern.compile("\\$\\{(((\\$[^\\{\\}])|[^\\$\\}])+\\$?)\\}");
        final Pattern backslash = Pattern.compile("\\\\");
        final Pattern dollar = Pattern.compile("\\$");

        if (s == null)
        {
            return null;
        }

        if (s.indexOf("${") == -1)
        { // shortcut if no expansions
            return s;
        }

        for (int i = 0; i < 20; i++)
        { // avoids self-referencing expansions
            // System.out.println("XXX expand[" + i + "] = [" + s + "]");
            Matcher matcher = propRef.matcher(s);

            if (!matcher.find())
            {
                // replace unmatched items
                s = s.replaceAll("\\Q??[\\E", "\\${");
                s = s.replaceAll("\\Q??]\\E", "}");
                // debug("expanded: " + s);
                if (s.indexOf("${") != -1)
                {
                    throw new RuntimeException("Can't expand: " + s);
                }
                return s;
            }

            String key = matcher.group(1);
            String[] keydef = key.split(":[=+-?@]", 2);
            String replace;

            if (keydef.length != 2)
            {
                replace = key.length() == 0 ? null : p.getProperty(key);
            }
            else
            {
                replace = keydef[0].length() == 0 ? null : p.getProperty(keydef[0]);

                if (replace != null
                    && (replace.length() == 0 || replace.indexOf("${") != -1))
                {
                    // don't want unexpanded replacement, as it may stop ${...:-default}
                    replace = null;
                }

                if (key.indexOf(":+") != -1)
                {
                    replace = ((replace == null) ? "" : keydef[1]);
                }
                else if (replace == null)
                {
                    replace = keydef[1];

                    if (key.indexOf(":?") != -1)
                    {
                        String msg = "${" + keydef[0] + ":?" + keydef[1]
                            + "} property not set";
                        throw new RuntimeException(msg);
                    }

                    if (key.indexOf(":=") != -1)
                    {
                        p.setProperty(keydef[0], keydef[1]);
                    }
                }
            }

            if (replace == null)
            {
                // TODO: this is a hack to avoid looping on unmatched references
                // should really leave unchanged and process rest of string.
                // We use "]" as delimiter to avoid non-matched "}"
                // terminating potential _propRef match
                replace = "??[" + key + "??]";
            }

            // Excerpt from replaceAll() javadoc:
            //
            // Note that backslashes (\) and dollar signs ($) in the replacement
            // string may cause the results to be different than if it were
            // being
            // treated as a literal replacement string. Dollar signs may be
            // treated
            // as references to captured subsequences, and backslashes are used
            // to
            // escape literal characters in the replacement string.
            // escape any \ or $ in replacement string
            replace = backslash.matcher(replace).replaceAll("\\\\\\\\");
            replace = dollar.matcher(replace).replaceAll("\\\\\\$");

            s = s.replaceAll("\\Q${" + key + "}\\E", replace);
        }

        throw new RuntimeException("expand: loop expanding: " + s);
    }

    public static String expand(String s)
    {
        return expand(s, BldProperties.global());
    }
}
