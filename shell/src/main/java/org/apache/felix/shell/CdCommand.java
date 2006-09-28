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
package org.apache.felix.shell;

/**
 * This interface defines the <tt>cd</tt> command service interface for the
 * Felix impl service. The <tt>cd</tt> command does not really change the
 * directory of the impl, rather it maintains a base URL for
 * simplifying URL entry.
 * <p>
 * For example, if the base URL is <tt>http://www.foo.com/<tt> and you
 * try to install a bundle <tt>foo.jar</tt>, the actual URL will be
 * expanded to <tt>http://www.foo.com/foo.jar</tt>. Any bundles wishing
 * to retrieve or set the current directory of the impl can use this
 * service interface.
**/
public interface CdCommand extends Command
{
    /**
     * Property used to configure the base URL.
    **/
    public static final String BASE_URL_PROPERTY = "felix.shell.baseurl";

    /**
     * Returns the current <i>directory</i> of the impl service.
     * @return the current impl directory.
    **/
    public String getBaseURL();

    /**
     * Sets the current <i>directory</i> of the impl service.
     * @param s the new value for the base URL.
    **/
    public void setBaseURL(String s);
}
