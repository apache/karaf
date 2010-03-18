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
package org.apache.felix.utils.filter;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;
import org.osgi.framework.Version;

public class FilterImplTest extends TestCase
{
    public void testStandardLDAP() throws Exception
    {
        FilterImpl filterImpl = FilterImpl.newInstance(
            "(&(package=org.eclipse.core.runtime)(version>=0.0.0)(common=split))");

        Dictionary dict = new Hashtable();
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", "0.0.0");
        dict.put("common", "split");

        assertTrue(filterImpl.match(dict));

        dict = new Hashtable();
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", "0.0.0");
        dict.put("common", "split-wrong");

        assertFalse(filterImpl.match(dict));
    }

    public void testNoneStandardLDAPOperators() throws Exception
    {
        FilterImpl filterImpl = FilterImpl.newInstance(
            "(&(package=org.eclipse.core.runtime)(version>=0.0.0)(common=split)(mandatory:<*common,test))");

        Dictionary dict = new Hashtable();
        dict.put("somethindifferent", "sonstwas");
        assertFalse(filterImpl.match(dict));

        dict = new Hashtable();
        dict.put("mandatory:", "common");
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", new Version("0.0.0"));
        dict.put("common", "split");
        assertTrue(filterImpl.match(dict));

        dict = new Hashtable();
        dict.put("mandatory:", "common,test");
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", new Version("0.0.0"));
        dict.put("common", "split");
        assertTrue(filterImpl.match(dict));

        dict = new Hashtable();
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", new Version("0.0.0"));
        dict.put("common", "split");
        assertTrue(filterImpl.match(dict));

        filterImpl = FilterImpl.newInstance(
            "(&(package=org.eclipse.core.runtime)(version>=0.0.0)(common=split)(mandatory:*>common))");
        dict = new Hashtable();
        dict.put("mandatory:", "common");
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", new Version("0.0.0"));
        dict.put("common", "split");
        assertTrue(filterImpl.match(dict));

        dict = new Hashtable();
        dict.put("mandatory:", "common,test");
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", new Version("0.0.0"));
        dict.put("common", "split");
        assertTrue(filterImpl.match(dict));

        filterImpl = FilterImpl.newInstance(
            "(&(package=org.eclipse.core.runtime)(version>=0.0.0)(common=split)(mandatory:*>common,test))");
        dict = new Hashtable();
        dict.put("mandatory:", "common");
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", new Version("0.0.0"));
        dict.put("common", "split");
        assertFalse(filterImpl.match(dict));

        dict = new Hashtable();
        dict.put("mandatory:", "common,test");
        dict.put("package", "org.eclipse.core.runtime");
        dict.put("version", new Version("0.0.0"));
        dict.put("common", "split");
        assertTrue(filterImpl.match(dict));
    }

    public void testCaseSensitive() throws Exception
    {
        FilterImpl filterImpl = FilterImpl.newInstance("(&(package=org.eclipse.core.runtime))");

        Dictionary dict = new Hashtable();
        dict.put("PACKAGE", "org.eclipse.core.runtime");
        assertTrue(filterImpl.match(dict));

        dict = new Hashtable();
        dict.put("PACKAGE", "org.eclipse.core.runtime");
        assertFalse(filterImpl.matchCase(dict));
    }

}