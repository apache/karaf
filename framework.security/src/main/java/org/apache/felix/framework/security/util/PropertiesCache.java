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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.felix.framework.util.SecureAction;

public final class PropertiesCache
{
    private final File m_file;

    private final File m_tmp;

    private final SecureAction m_action;

    public PropertiesCache(File store, File tmp, SecureAction action)
    {
        m_action = action;
        m_file = store;
        m_tmp = tmp;
    }

    public void write(Map data) throws IOException
    {
        OutputStream out = null;
        File tmp = null;
        File tmp2 = null;
        try
        {
            tmp = m_action.createTempFile("tmp", null, m_tmp);
            tmp2 = m_action.createTempFile("tmp", null, m_tmp);
            m_action.deleteFile(tmp2);
            Exception org = null;
            try
            {
                out = m_action.getFileOutputStream(tmp);

                Properties store = new Properties();

                int count = 0;

                for (Iterator iter = data.entrySet().iterator(); iter.hasNext();)
                {
                    Entry entry = (Entry) iter.next();
                    store.setProperty(count++ + "-" + (String) entry.getKey(),
                        getEncoded(entry.getValue()));
                }

                store.store(out, null);
            }
            catch (IOException ex)
            {
                org = ex;
                throw ex;
            }
            finally
            {
                if (out != null)
                {
                    try
                    {
                        out.close();
                    }
                    catch (IOException ex)
                    {
                        if (org == null)
                        {
                            throw ex;
                        }
                    }
                }
            }
            if ((m_action.fileExists(m_file) && !m_action.renameFile(m_file,
                tmp2))
                || !m_action.renameFile(tmp, m_file))
            {
                throw new IOException("Unable to write permissions");
            }
        }
        catch (IOException ex)
        {
            if (!m_action.fileExists(m_file) && (tmp2 != null)
                && m_action.fileExists(tmp2))
            {
                m_action.renameFile(tmp2, m_file);
            }
            throw ex;
        }
        finally
        {
            if (tmp != null)
            {
                m_action.deleteFile(tmp);
            }
            if (tmp2 != null)
            {
                m_action.deleteFile(tmp2);
            }
        }
    }

    public void read(Class target, Map map) throws IOException
    {
        if (!m_file.isFile())
        {
            return;
        }
        InputStream in = null;
        Exception other = null;
        Map result = new TreeMap();
        try
        {
            in = m_action.getFileInputStream(m_file);

            Properties store = new Properties();
            store.load(in);

            for (Iterator iter = store.entrySet().iterator(); iter.hasNext();)
            {
                Entry entry = (Entry) iter.next();
                result.put(entry.getKey(), getUnencoded((String) entry
                    .getValue(), target));
            }
        }
        catch (IOException ex)
        {
            other = ex;
            throw ex;
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ex)
                {
                    if (other == null)
                    {
                        throw ex;
                    }
                }
            }
        }
        for (Iterator iter = result.entrySet().iterator(); iter.hasNext();)
        {
            Entry entry = (Entry) iter.next();
            String key = (String) entry.getKey();
            map.put(key.substring(key.indexOf("-")), entry.getValue());
        }
    }

    private String getEncoded(Object target) throws IOException
    {
        Properties props = new Properties();
        if (target.getClass().isArray())
        {

            Object[] array = (Object[]) target;
            for (int i = 0; i < array.length; i++)
            {
                props.setProperty(Integer.toString(i), array[i].toString());
            }

            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            props.store(tmp, null);
            return new String(tmp.toByteArray());
        }

        return target.toString();
    }

    private Object getUnencoded(String encoded, Class target)
        throws IOException
    {
        try
        {
            if (target.isArray())
            {
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(encoded.getBytes()));
                Class componentType = target.getComponentType();
                Constructor constructor = m_action.getConstructor(
                    componentType, new Class[] { String.class });
                Object[] params = new Object[1];
                Object[] result = (Object[]) Array.newInstance(componentType,
                    props.size());

                for (Iterator iter = props.entrySet().iterator(); iter
                    .hasNext();)
                {
                    Entry entry = (Entry) iter.next();
                    params[0] = entry.getValue();
                    result[Integer.parseInt((String) entry.getKey())] = constructor
                        .newInstance(params);
                }

                return result;
            }

            return m_action.invoke(m_action.getConstructor(target,
                new Class[] { String.class }), new Object[] { encoded });
        }
        catch (Exception ex)
        {
            ex.printStackTrace();

            throw new IOException(ex.getMessage());
        }
    }
}
