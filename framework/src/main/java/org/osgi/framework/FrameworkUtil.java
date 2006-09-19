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
package org.osgi.framework;

import org.apache.felix.framework.FilterImpl;

/**
 * <p>
 * Framework utility class that currently only provides public access to
 * <tt>Filter</tt> instance creation. This class is a replacement for the
 * version that ships with the standard OSGi JAR file.
 * </p>
**/
public class FrameworkUtil
{
    private FrameworkUtil()
    {
    }

    public static Filter createFilter(String filter)
        throws InvalidSyntaxException
    {
        return new FilterImpl(null, filter);
    }
}