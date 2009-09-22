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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.felix.sigil.core.util.QuoteUtil;

public class BldConfig
{
    // control properties
    public static final String C_BUNDLES = "-bundles";
    public static final String C_REPOSITORIES = "-repositories";

    // string properties
    public static final String S_ACTIVATOR = "-activator";
    public static final String S_DEFAULTS = "-defaults";
    public static final String S_SINGLETON = "-singleton";
    public static final String S_ID = "id";
    public static final String S_SYM_NAME = "name";
    public static final String S_VERSION = "version";
    public static final String[] STRING_KEYS = { S_ACTIVATOR, S_DEFAULTS, S_ID,
            S_SYM_NAME, S_VERSION, S_SINGLETON };

    // list properties
    public static final String L_CONTENTS = "-contents";
    public static final String L_SRC_CONTENTS = "-sourcedirs";
    public static final String L_RESOURCES = "-resources";
    public static final String[] LIST_KEYS = { L_CONTENTS, L_SRC_CONTENTS, L_RESOURCES };

    // map properties
    public static final String M_EXPORTS = "-exports";
    public static final String M_IMPORTS = "-imports";
    public static final String M_REQUIRES = "-requires";
    public static final String M_FRAGMENT = "-fragment";
    public static final String M_LIBS = "-libs";
    public static final String[] MAP_KEYS = { M_EXPORTS, M_IMPORTS, M_REQUIRES,
            M_FRAGMENT, M_LIBS };

    // property properties
    public static final String P_HEADER = "header";
    public static final String P_OPTION = "option";
    public static final String P_PACKAGE_VERSION = "package";
    public static final String P_BUNDLE_VERSION = "bundle";
    public static final String[] PROP_KEYS = { P_HEADER, P_OPTION, P_PACKAGE_VERSION,
            P_BUNDLE_VERSION };

    // private constants
    private static final String LIST_REGEX = ",\\s*";
    private static final String MAPATTR_REGEX = ";\\s*";
    private static final String MAPATTR_SEP = ";";
    private static final String SUBKEY_SEP = ";";

    // configuration is stored in typed maps
    private Map<String, String> string = new TreeMap<String, String>();
    private Map<String, List<String>> list = new TreeMap<String, List<String>>();
    private Map<String, Map<String, Map<String, String>>> map = new TreeMap<String, Map<String, Map<String, String>>>();
    private Map<String, BldConfig> config = new TreeMap<String, BldConfig>();
    private Map<String, Properties> property = new TreeMap<String, Properties>();

    // default config - not modified or saved
    private BldConfig dflt;

    private Properties unknown = new Properties();
    private String comment = "";

    public BldConfig()
    {
    }

    public BldConfig(Properties p) throws IOException
    {
        merge(p);
    }

    public void setDefault(BldConfig dflt)
    {
        this.dflt = dflt;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public Properties getUnknown()
    {
        return unknown;
    }

    public String getString(String id, String key)
    {
        if (id != null && config.containsKey(id))
        {
            String value = config.get(id).getString(null, key);
            if (value != null)
                return value;
        }
        return string.containsKey(key) ? string.get(key)
            : (dflt != null ? dflt.getString(id, key) : null);
    }

    public List<String> getList(String id, String key)
    {
        if (id != null && config.containsKey(id))
        {
            List<String> value = config.get(id).getList(null, key);
            if (value != null)
                return value;
        }
        return list.containsKey(key) ? list.get(key) : (dflt != null ? dflt.getList(id,
            key) : Collections.<String> emptyList());
    }

    public Map<String, Map<String, String>> getMap(String id, String key)
    {
        if (id != null && config.containsKey(id))
        {
            Map<String, Map<String, String>> value = config.get(id).getMap(null, key);
            if (value != null)
                return value;
        }
        return map.containsKey(key) ? map.get(key) : (dflt != null ? dflt.getMap(id, key)
            : Collections.<String, Map<String, String>> emptyMap());
    }

    public void setString(String id, String key, String value)
    {
        if (!value.equals(getString(id, key)))
        {
            if (id != null)
            {
                if (!config.containsKey(id))
                    config.put(id, new BldConfig());
                config.get(id).setString(null, key, value);
            }
            else
            {
                String dval = (dflt == null ? dflt.getString(null, key) : null);
                if (value.equals("") && (dval == null || dval.equals("")))
                {
                    string.remove(key);
                }
                else
                {
                    string.put(key, value);
                }
            }
        }
    }

    public void setList(String id, String key, List<String> value)
    {
        if (!value.equals(getList(id, key)))
        {
            if (id != null)
            {
                if (!config.containsKey(id))
                    config.put(id, new BldConfig());
                config.get(id).setList(null, key, value);
            }
            else if (value.isEmpty()
                && (dflt == null || dflt.getList(null, key).isEmpty()))
            {
                list.remove(key);
            }
            else
            {
                list.put(key, value);
            }
        }
    }

    public void setMap(String id, String key, Map<String, Map<String, String>> value)
    {
        if (!value.equals(getMap(id, key)))
        {
            if (id != null)
            {
                if (!config.containsKey(id))
                    config.put(id, new BldConfig());
                config.get(id).setMap(null, key, value);
            }
            else if (value.isEmpty()
                && (dflt == null || dflt.getMap(null, key).isEmpty()))
            {
                map.remove(key);
            }
            else
            {
                map.put(key, value);
            }
        }
    }

    public Properties getProps(String id, String key)
    {
        // merge main and sub properties
        Properties props = new Properties();

        if (dflt != null)
            props.putAll(dflt.getProps(id, key));

        if (property.containsKey(key))
            props.putAll(property.get(key));

        if (id != null && config.containsKey(id))
        {
            Properties p2 = config.get(id).getProps(null, key);
            if (p2 != null)
                props.putAll(p2);
        }

        return props;
    }

    // only sets one property at a time
    public void setProp(String id, String key, String k2, String v2)
    {
        if (v2 == null)
            return;
        Properties props = getProps(id, key);
        if (!v2.equals(props.getProperty(key)))
        {
            if (id != null)
            {
                if (!config.containsKey(id))
                    config.put(id, new BldConfig());
                config.get(id).setProp(null, key, k2, v2);
            }
            else
            {
                if (property.containsKey(key))
                {
                    property.get(key).put(k2, v2);
                }
                else
                {
                    Properties value = new Properties();
                    value.put(k2, v2);
                    property.put(key, value);
                }
            }
        }
    }

    /**
     * write config in Property file format.
     * This allows us to make it prettier than Properties.store().
     */
    public void write(final PrintWriter out)
    {
        out.println(comment);

        // Note: don't add date stamp, or file will differ each time it's saved.
        out.println("# sigil project file, saved by plugin.\n");

        dump("", new Properties()
        {
            private static final long serialVersionUID = 1L; //appease eclipse

            @Override
            public Object put(Object key, Object value)
            {
                if (value instanceof String)
                {
                    out.println(key + ": " + value);
                    out.println("");
                }
                else if (value instanceof List)
                {
                    out.println(key + ": \\");
                    for (Object k : (List<?>) value)
                    {
                        out.println("\t" + k + ", \\");
                    }
                    out.println("");
                }
                else if (value instanceof Map)
                {
                    out.println(key + ": \\");
                    StringBuilder b = new StringBuilder();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet())
                    {
                        b.append("\t");
                        b.append(e.getKey());
                        Map<?, ?> v = (Map<?, ?>) e.getValue();
                        if (!v.isEmpty())
                        {
                            for (Map.Entry<?, ?> e2 : v.entrySet())
                            {
                                b.append(MAPATTR_SEP);
                                b.append(e2.getKey());
                                b.append("=");
                                String v2 = e2.getValue().toString();
                                if (v2.contains(","))
                                {
                                    b.append("\"");
                                    b.append(v2);
                                    b.append("\"");
                                }
                                else
                                {
                                    b.append(v2);
                                }
                            }
                        }
                        b.append(", \\\n");
                    }
                    out.println(b.toString());
                }
                return null;
            }
        });
        out.println("# end");
    }

    /**
     * dump config in pseudo Properties format.
     * Note: some values are not Strings (they're List<String>).
     */
    private void dump(String prefix, Properties p)
    {
        for (String key : string.keySet())
        {
            p.put(prefix + key, string.get(key));
        }

        for (String key : list.keySet())
        {
            List<String> list2 = list.get(key);
            p.put(prefix + key, list2);
        }

        for (String key : map.keySet())
        {
            Map<String, Map<String, String>> map2 = map.get(key);
            p.put(prefix + key, map2);
        }

        for (String key : property.keySet())
        {
            Properties props = property.get(key);
            for (Object k2 : props.keySet())
            {
                p.put(prefix + key + SUBKEY_SEP + k2, props.get(k2));
            }
        }

        for (String key : config.keySet())
        {
            BldConfig config2 = config.get(key);
            config2.dump(key + SUBKEY_SEP + prefix, p);
        }

        for (Object key : unknown.keySet())
        {
            String value = unknown.getProperty((String) key);
            if (value.length() > 0)
                p.put(prefix + key, value);
        }
    }

    /**
     * merges properties into current configuration.
     * @param p
     * @throws IOException 
     */
    public void merge(Properties p) throws IOException
    {
        if (p.isEmpty())
            return;

        final List<String> strings = Arrays.asList(STRING_KEYS);
        final List<String> lists = Arrays.asList(LIST_KEYS);
        final List<String> maps = Arrays.asList(MAP_KEYS);

        List<String> bundleKeys = new ArrayList<String>();
        List<String> repoKeys = new ArrayList<String>();

        String bundles = p.getProperty(C_BUNDLES);
        if (bundles != null)
        {
            bundleKeys.addAll(Arrays.asList(bundles.split(LIST_REGEX)));
            list.put(C_BUNDLES, bundleKeys);
        }

        String repos = p.getProperty(C_REPOSITORIES);
        if (repos != null && !list.containsKey(C_REPOSITORIES))
        {
            for (String s : repos.split(LIST_REGEX))
            {
                repoKeys.add(s.trim());
            }
            list.put(C_REPOSITORIES, repoKeys);
        }

        List<String> subKeys = new ArrayList<String>();
        subKeys.addAll(Arrays.asList(PROP_KEYS));
        subKeys.addAll(bundleKeys);
        subKeys.addAll(repoKeys);

        Map<String, Properties> sub = new TreeMap<String, Properties>();

        for (Object k : p.keySet())
        {
            String key = (String) k;
            if (key.equals(C_BUNDLES) || key.equals(C_REPOSITORIES))
                continue;

            String[] keys = key.split(SUBKEY_SEP, 2);
            String value = p.getProperty(key);

            if (keys.length > 1)
            {
                Properties p2 = sub.get(keys[0]);
                if (p2 == null)
                {
                    p2 = new Properties();
                    sub.put(keys[0], p2);
                    if (!subKeys.contains(keys[0]))
                    {
                        // unknown.setProperty(keys[0] + SUBKEY_SEP, "");
                    }
                }
                p2.setProperty(keys[1], value);
            }
            else if (strings.contains(key))
            {
                if (!string.containsKey(key))
                    string.put(key, value);
            }
            else if (lists.contains(key))
            {
                if (!list.containsKey(key))
                {
                    ArrayList<String> list2 = new ArrayList<String>();
                    for (String s : value.split(LIST_REGEX))
                    {
                        if (s.trim().length() > 0)
                        {
                            list2.add(s.trim());
                        }
                    }
                    if (!list2.isEmpty())
                    {
                        list.put(key, list2);
                    }
                }
            }
            else if (maps.contains(key))
            {
                if (!map.containsKey(key))
                {
                    Map<String, Map<String, String>> map2 = new TreeMap<String, Map<String, String>>();

                    for (String subValue : QuoteUtil.split(value))
                    {
                        if (subValue.trim().length() > 0)
                        {
                            String[] split = subValue.split(MAPATTR_REGEX);
                            Map<String, String> map3 = new TreeMap<String, String>();
                            for (int i = 1; i < split.length; ++i)
                            {
                                String[] keyVal = split[i].split(":?=", 2);
                                if (keyVal.length != 2)
                                {
                                    throw new IOException("attribute missing '=':"
                                        + subValue);
                                }
                                map3.put(keyVal[0], keyVal[1]);
                            }
                            map2.put(split[0], map3);
                        }
                    }

                    map.put(key, map2);
                }
            }
            else
            {
                unknown.setProperty(key, value);
            }
        }

        for (String subKey : sub.keySet())
        {
            Properties props = sub.get(subKey);
            if (!props.isEmpty())
            {
                if (bundleKeys.contains(subKey))
                {
                    BldConfig config2 = new BldConfig(props);
                    Properties unkProps = config2.getUnknown();

                    if (config2.map.containsKey(M_IMPORTS))
                        unkProps.setProperty(M_IMPORTS, "");

                    if (config2.map.containsKey(M_REQUIRES))
                        unkProps.setProperty(M_REQUIRES, "");

                    for (Object unk : unkProps.keySet())
                    {
                        unknown.setProperty(subKey + SUBKEY_SEP + unk, "");
                    }
                    config.put(subKey, config2);
                }
                else
                {
                    Properties p2 = property.get(subKey);
                    if (p2 == null)
                    {
                        property.put(subKey, props);
                    }
                    else
                    {
                        for (Object k : props.keySet())
                        {
                            if (!p2.containsKey(k))
                            {
                                p2.put(k, props.get(k));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return "STRING: " + string + " LIST:" + list + " MAP: " + map + " PROPERTY: "
            + property + " CONFIG:" + config + "\nDFLT{ " + dflt + "}";
    }

}
