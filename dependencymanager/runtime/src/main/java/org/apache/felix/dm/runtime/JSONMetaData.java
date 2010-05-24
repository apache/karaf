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
package org.apache.felix.dm.runtime;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A JSON MetaData.
 */
public class JSONMetaData implements MetaData, Cloneable
{
    /**
     * The parsed Dependency or Service metadata. The map value is either a String, a String[],
     * or a Dictionary, whose values are String or String[]. 
     */
    private HashMap<String, Object> m_metadata = new HashMap<String, Object>();

    /**
     * Decodes a JSON metadata for either a Service or a Dependency descriptor entry.
     * The JSON object values contains either some of the following types: a String, a String[], or a Dictionary of String/String[].
     * @param jso the JSON object that corresponds to a dependency manager descriptor entry line.
     * @throws JSONException 
     */
    @SuppressWarnings("unchecked")
    public JSONMetaData(JSONObject jso) throws JSONException
    {
        // Decode json object into our internal map.
        Iterator<String> it = jso.keys();
        while (it.hasNext())
        {
            String key = it.next();
            Object value = jso.get(key);
            if (value instanceof String)
            {
                m_metadata.put(key, value);
            }
            else if (value instanceof JSONArray)
            {
                String[] array = decodeStringArray((JSONArray) value);
                m_metadata.put(key, array);
            }
            else if (value instanceof JSONObject)
            {
                Hashtable<String, Object> h = new Hashtable<String, Object>();
                JSONObject obj = ((JSONObject) value);
                Iterator<String> it2 = obj.keys();
                while (it2.hasNext())
                {
                    String key2 = it2.next();
                    Object value2 = obj.get(key2);
                    if (value2 instanceof String)
                    {
                        h.put(key2, value2);
                    }
                    else if (value2 instanceof JSONArray)
                    {
                        String[] array = decodeStringArray((JSONArray) value2);
                        h.put(key2, array);
                    }
                    else
                    {
                        throw new IllegalArgumentException("Could not decode JSON metadata: key " + key +
                                "contains an invalid dictionary key: " + key
                                + " (the value is neither a String nor a String[]).");
                    }
                }
                m_metadata.put(key, h);
            }
        }
    }

    /**
     * Close this class instance to another one.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        JSONMetaData clone = (JSONMetaData) super.clone();
        clone.m_metadata = (HashMap<String, Object>) m_metadata.clone();
        return clone;
    }

    public String getString(Params key)
    {
        String value = (String) m_metadata.get(key.toString());
        if (value == null)
        {
            throw new IllegalArgumentException("Parameter " + key + " not found");
        }
        return value;
    }

    public String getString(Params key, String def)
    {
        try
        {
            return getString(key);
        }
        catch (IllegalArgumentException e)
        {
            return def;
        }
    }

    public int getInt(Params key)
    {
        String value = getString(key, null);
        if (value != null)
        {
            try
            {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not an int value: "
                    + value);
            }
        }
        else
        {
            throw new IllegalArgumentException("missing " + key
                + " parameter from annotation");
        }
    }

    public int getInt(Params key, int def)
    {
        String value = getString(key, null);
        if (value != null)
        {
            try
            {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not an int value: "
                    + value);
            }
        }
        else
        {
            return def;
        }
    }

    public long getLong(Params key)
    {
        String value = getString(key, null);
        if (value != null)
        {
            try
            {
                return Long.parseLong(value);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not a long value: "
                    + value);
            }
        }
        else
        {
            throw new IllegalArgumentException("missing " + key
                + " parameter from annotation");
        }
    }

    public long getLong(Params key, long def)
    {
        String value = getString(key, null);
        if (value != null)
        {
            try
            {
                return Long.parseLong(value);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not a long value: "
                    + value);
            }
        }
        else
        {
            return def;
        }
    }

    public String[] getStrings(Params key)
    {
        Object array = m_metadata.get(key.toString());
        if (array == null)
        {
            throw new IllegalArgumentException("Parameter " + key + " not found");
        }

        if (!(array instanceof String[]))
        {
            throw new IllegalArgumentException("Parameter " + key + " is not a String[] (" + array.getClass()
                + ")");
        }
        return (String[]) array;
    }

    public String[] getStrings(Params key, String[] def)
    {
        try
        {
            return getStrings(key);
        }
        catch (IllegalArgumentException t)
        {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    public Dictionary<String, Object> getDictionary(Params key,
        Dictionary<String, Object> def)
    {
        Object dictionary = m_metadata.get(key.toString());
        if (dictionary == null)
        {
            return def;
        }

        if (!(dictionary instanceof Dictionary<?, ?>))
        {
            throw new IllegalArgumentException("Parameter " + key + " is not a Dictionary ("
                + dictionary.getClass() + ")");
        }

        return (Dictionary<String, Object>) dictionary;
    }

    @Override
    public String toString()
    {
        return m_metadata.toString();
    }

    public void setDictionary(Params key, Dictionary<String, Object> dictionary)
    {
        m_metadata.put(key.toString(), dictionary);
    }

    public void setString(Params key, String value)
    {
        m_metadata.put(key.toString(), value);
    }

    public void setStrings(Params key, String[] values)
    {
        m_metadata.put(key.toString(), values);
    }

    /**
     * Decodes a JSONArray into a String array (all JSON array values are supposed to be strings).
     */
    private String[] decodeStringArray(JSONArray array) throws JSONException
    {
        String[] arr = new String[array.length()];
        for (int i = 0; i < array.length(); i++)
        {
            Object value = array.get(i);
            if (!(value instanceof String))
            {
                throw new IllegalArgumentException("JSON array is not an array of Strings: " + array);
            }
            arr[i] = value.toString();
        }
        return arr;
    }
}
