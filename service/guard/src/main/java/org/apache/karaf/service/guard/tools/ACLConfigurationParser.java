/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.service.guard.tools;

import java.util.*;

import org.apache.karaf.service.guard.impl.GuardProxyCatalog;

public class ACLConfigurationParser {

    // note that the order of the enums is important. Needs to be from most specific to least specific.
    public enum Specificity {
        ARGUMENT_MATCH,
        SIGNATURE_MATCH,
        NAME_MATCH,
        WILDCARD_MATCH,
        NO_MATCH
    }

    static String compulsoryRoles;
    
    static {
        compulsoryRoles = System.getProperty(GuardProxyCatalog.KARAF_SECURED_COMMAND_COMPULSORY_ROLES_PROPERTY);
    }
    
    /**
     * <p>Returns the roles that can invoke the given operation. This is determined by matching the
     * operation details against configuration provided.</p>
     *
     * <p>The following configuration is supported. Keys are used to match an invocation against. The value can contain
     * a comma-separated list of roles. Spaces are ignored for the role values. Note that comments are allowed in the
     * value field after the hash {@code #} character:</p>
     *
     * <pre>
     *     {@code
     *     myMethod = role1, role2
     *     methodName(int)[/17/] = role1                    # regex match, assume it's surrounded by ^ and $
     *     methodName(int)[/[01]8/] = role2
     *     methodName(int)["19"] = role3                    # exact value match
     *     methodName(int) = role4                          # signature match
     *     methodName(java.lang.String, int) = role5        # signature match
     *     methodName =                                     # no roles can invoke this command
     *     method* = role6                                  # name prefix/wildcard match. The asterisk must appear at the end.
     *     }
     * </pre>
     *
     * <p>The following algorithm is used to find matching roles:</p>
     * <ol>
     *     <li>Find all regex and exact value matches. For all parameters these matches are found by calling {@code toString()}
     *         on the parameters passed in. If there are multiple matches in this category all the matching roles are collected.
     *         If any is found return these roles.
     *     </li>
     *     <li>Find a signature match. If found return the associated roles.</li>
     *     <li>Find a method name match. If found return the associated roles.</li>
     *     <li>Find a method name prefix/wildcard match. If more than one prefix match, the roles associated with the longest
     *         prefix is used. So for example, if there are rules for {@code get*} and {@code *} only the roles associated with
     *         {@code get*} are returned.
     *     </li>
     *     <li>If none of the above criteria match, this method returns {@code null}.</li>
     * </ol>
     *
     * @param methodName the method name to be invoked.
     * @param params the parameters provided for the invocation. May be {@code null} for cases there the parameters are not yet
     *               known. In this case the roles that can <em>potentially</em> invoke the method are returned, although based on
     *               parameter values the actual invocation may still be denied.
     * @param signature the signature of the method specified as an array of class name. For simple types, the simple type name
     *                  is used (e.g. "int").
     * @param config the configuration to check against.
     * @param addToRoles the list of roles (which may be empty) if a matching configuration iteam has been found.
     * @return the specificity
     */
    public static Specificity getRolesForInvocation(String methodName, Object[] params, String[] signature,
                                                    Dictionary<String, Object> config, List<String> addToRoles) {
        Dictionary<String, Object> properties = trimKeys(config);
        String pid = (String)properties.get("service.pid");
        Specificity s = getRolesBasedOnSignature(methodName, params, signature, properties, addToRoles);
        if (s != Specificity.NO_MATCH) {
            return s;
        }

        s = getRolesBasedOnSignature(methodName, params, null, properties, addToRoles);
        if (s != Specificity.NO_MATCH) {
            return s;
        }

        List<String> roles = getMethodNameWildcardRoles(properties, methodName);
        if (roles != null) {
            addToRoles.addAll(roles);
            return Specificity.WILDCARD_MATCH;
        } else if (compulsoryRoles != null && !pid.contains("jmx.acl")){
            addToRoles.addAll(ACLConfigurationParser.parseRoles(compulsoryRoles));
            return Specificity.NAME_MATCH;
        } else {
            return Specificity.NO_MATCH;
        }
            
    }
    
    public static Specificity getRolesForInvocationForAlias(String methodName, Object[] params, String[] signature,
                                                    Dictionary<String, Object> config, List<String> addToRoles) {
        Dictionary<String, Object> properties = trimKeys(config);
        String pid = (String)properties.get("service.pid");
        Specificity s = getRolesBasedOnSignature(methodName, params, signature, properties, addToRoles);
        if (s != Specificity.NO_MATCH) {
            return s;
        }

        s = getRolesBasedOnSignature(methodName, params, null, properties, addToRoles);
        if (s != Specificity.NO_MATCH) {
            return s;
        }

        List<String> roles = getMethodNameWildcardRoles(properties, methodName);
        if (roles != null) {
            addToRoles.addAll(roles);
            return Specificity.WILDCARD_MATCH;
        } else {
            return Specificity.NO_MATCH;
        }
            
    }
    
    public static void getCompulsoryRoles(List<String> roles) {
        if (compulsoryRoles != null) {
            roles.addAll(ACLConfigurationParser.parseRoles(compulsoryRoles));
        }
    }

    private static Specificity getRolesBasedOnSignature(String methodName, Object[] params, String[] signature,
                                                        Dictionary<String, Object> properties, List<String> roles) {
        if (params != null) {
            boolean foundExactOrRegex = false;
            Object exactArgMatchRoles = properties.get(getExactArgSignature(methodName, signature, params));
            if (exactArgMatchRoles instanceof String) {
                roles.addAll(parseRoles((String) exactArgMatchRoles));
                foundExactOrRegex = true;
            }

            List<String> r = getRegexRoles(properties, methodName, signature, params);
            if (r != null) {
                foundExactOrRegex = true;
                roles.addAll(r);
            }

            if (foundExactOrRegex) {
                // since we have the actual parameters we can match them and if they do we won't look for any
                // more generic rules...
                return Specificity.ARGUMENT_MATCH;
            }
        } else {
            // this is used in the case where parameters aren't known yet and the system wants to find out
            // what roles in principle can invoke this method
            List<String> r = getExactArgOrRegexRoles(properties, methodName, signature);
            if (r != null) {
               roles.addAll(r);
            }
        }

        Object signatureRoles = properties.get(getSignature(methodName, signature));
        if (signatureRoles instanceof String) {
            roles.addAll(parseRoles((String) signatureRoles));
            return signature == null ? Specificity.NAME_MATCH : Specificity.SIGNATURE_MATCH;
        }

        return Specificity.NO_MATCH;
    }

    private static Dictionary<String, Object> trimKeys(Dictionary<String, Object> properties) {
        Dictionary<String, Object> d = new Hashtable<>();
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            Object value = properties.get(key);
            d.put(removeSpaces(key), value);
        }
        return d;
    }

    private static String removeSpaces(String key) {
        StringBuilder sb = new StringBuilder();
        char quoteChar = 0;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);

            if (quoteChar == 0 && c == ' ')
                continue;

            if (quoteChar == 0 && (c == '\"' || c == '/') && sb.length() > 0 &&
                    (sb.charAt(sb.length() - 1) == '[' || sb.charAt(sb.length() - 1) == ',')) {
                // we're in a quoted string
                quoteChar = c;
            } else if (quoteChar != 0 && c == quoteChar) {
                // look ahead to see if the next non-space is the closing bracket or a comma, which ends the quoted string
                for (int j = i + 1; j < key.length(); j++) {
                    if (key.charAt(j) == ' ')
                        continue;
                    if (key.charAt(j) == ']' || key.charAt(j) == ',')
                        quoteChar = 0;
                    break;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static List<String> parseRoles(String roleStr) {
        int hashIdx = roleStr.indexOf('#');
        if (hashIdx >= 0) {
            // you can put a comment at the end
            roleStr = roleStr.substring(0, hashIdx);
        }

        List<String> roles = new ArrayList<>();
        for (String role : roleStr.split("[,]")) {
            String trimmed = role.trim();
            if (trimmed.length() > 0) {
                roles.add(trimmed);
            }
        }

        return roles;
    }

    private static Object getExactArgSignature(String methodName, String[] signature, Object[] params) {
        StringBuilder sb = new StringBuilder(getSignature(methodName, signature));
        sb.append('[');
        boolean first = true;
        for (Object param : params) {
            if (first)
                first = false;
            else
                sb.append(',');
            sb.append('"');
            if (param != null)
                sb.append(param.toString().trim());
            sb.append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String getSignature(String methodName, String[] signature) {
        StringBuilder sb = new StringBuilder(methodName);
        if (signature == null)
            return sb.toString();

        sb.append('(');
        boolean first = true;
        for (String s : signature) {
            if (first)
                first = false;
            else
                sb.append(',');
            sb.append(s);
        }
        sb.append(')');
        return sb.toString();
    }

    private static List<String> getRegexRoles(Dictionary<String, Object> properties, String methodName, String[] signature, Object[] params) {
        List<String> roles = new ArrayList<>();
        boolean matchFound = false;
        String methodSig = getSignature(methodName, signature);
        String prefix = methodSig + "[/";
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement().trim();
            if (key.startsWith(prefix) && key.endsWith("/]")) {
                List<String> regexArgs = getRegexDecl(key.substring(methodSig.length()));
                if (allParamsMatch(regexArgs, params)) {
                    matchFound = true;
                    Object roleStr = properties.get(key);
                    if (roleStr instanceof String) {
                        roles.addAll(parseRoles((String) roleStr));
                    }
                }
            }
        }
        return matchFound ? roles : null;
    }

    private static List<String> getExactArgOrRegexRoles(Dictionary<String, Object> properties, String methodName, String[] signature) {
        List<String> roles = new ArrayList<>();
        boolean matchFound = false;
        String methodSig = getSignature(methodName, signature);
        String prefix = methodSig + "[";
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement().trim();
            if (key.startsWith(prefix) && key.endsWith("]")) {
                matchFound = true;
                Object roleStr = properties.get(key);
                if (roleStr instanceof String) {
                    roles.addAll(parseRoles((String) roleStr));
                }
            }
        }
        return matchFound ? roles : null;
    }

    private static List<String> getMethodNameWildcardRoles(Dictionary<String, Object> properties, String methodName) {
        SortedMap<String, String> wildcardRules = new TreeMap<>((s1, s2) -> {
            // returns longer entries before shorter ones...
            return s2.length() - s1.length();
        });
        
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            if (key.endsWith("*")) {
                String prefix = key.substring(0, key.length() - 1);
                if (methodName.startsWith(prefix)) {
                    wildcardRules.put(prefix, properties.get(key).toString());
                }
            }
            if (key.startsWith("*")) {
                String suffix = key.substring(1);
                if (methodName.endsWith(suffix)) {
                    wildcardRules.put(suffix, properties.get(key).toString());
                }
            }
            if (key.startsWith("*") && key.endsWith("*") && key.length() > 1) {
                String middle = key.substring(1, key.length() - 1);
                if (methodName.contains(middle)) {
                    wildcardRules.put(middle, properties.get(key).toString());
                }
            }
        }

        if (wildcardRules.size() != 0) {
            return parseRoles(wildcardRules.values().iterator().next());
        } else {
            return null;
        }
    }

    private static boolean allParamsMatch(List<String> regexArgs, Object[] params) {
        if (regexArgs.size() != params.length)
            return false;

        for (int i = 0; i < regexArgs.size(); i++) {
            if (params[i] == null)
                return false;
            if (!params[i].toString().trim().matches(regexArgs.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static List<String> getRegexDecl(String key) {
        List<String> l = new ArrayList<>();

        boolean inRegex = false;
        StringBuilder curRegex = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            if (!inRegex) {
                if (key.length() > i + 1) {
                    String s = key.substring(i, i + 2);
                    if ("[/".equals(s) || ",/".equals(s)) {
                        inRegex = true;
                        i++;
                        continue;
                    }
                }
            } else {
                String s = key.substring(i, i + 2);
                if ("/]".equals(s) || "/,".equals(s)) {
                    l.add(curRegex.toString());
                    curRegex = new StringBuilder();
                    inRegex = false;
                    continue;
                }
                curRegex.append(key.charAt(i));
            }
        }
        return l;
    }
}
