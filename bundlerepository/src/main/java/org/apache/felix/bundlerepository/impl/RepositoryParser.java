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
package org.apache.felix.bundlerepository.impl;

import java.io.InputStream;
import java.io.Reader;

public abstract class RepositoryParser
{
    public static final String REPOSITORY = "repository";
    public static final String NAME = "name";
    public static final String LASTMODIFIED = "lastmodified";
    public static final String REFERRAL = "referral";
    public static final String RESOURCE = "resource";
    public static final String DEPTH = "depth";
    public static final String URL = "url";
    public static final String CATEGORY = "category";
    public static final String ID = "id";
    public static final String CAPABILITY = "capability";
    public static final String REQUIRE = "require";
    public static final String P = "p";
    public static final String N = "n";
    public static final String T = "t";
    public static final String V = "v";
    public static final String FILTER = "filter";
    public static final String EXTEND = "extend";
    public static final String MULTIPLE = "multiple";
    public static final String OPTIONAL = "optional";

    public static final String OBR_PARSER_CLASS = "obr.xml.class";

    public static RepositoryParser getParser()
    {
        RepositoryParser parser = null;
        try
        {
            String className = Activator.getContext() != null
                                    ? Activator.getContext().getProperty(OBR_PARSER_CLASS)
                                    : System.getProperty(OBR_PARSER_CLASS);
            if (className != null && className.length() > 0)
            {
                parser = (RepositoryParser) Class.forName(className).newInstance();
            }
        }
        catch (Throwable t)
        {
            // Ignore
        }
        if (parser == null)
        {
            parser = new PullParser();

        }
        return parser;
    }


    public abstract RepositoryImpl parseRepository(InputStream is) throws Exception;

    public abstract RepositoryImpl parseRepository(Reader r) throws Exception;

    public abstract ResourceImpl parseResource(Reader reader) throws Exception;

    public abstract CapabilityImpl parseCapability(Reader reader) throws Exception;

    public abstract PropertyImpl parseProperty(Reader reader) throws Exception;

    public abstract RequirementImpl parseRequirement(Reader reader) throws Exception;

}
