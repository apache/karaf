/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.felix.sigil.common.runtime.cli;

/**
 * A class that implements the <code>CommandLineParser</code> interface
 * can parse a String array according to the {@link Options} specified
 * and return a {@link CommandLine}.
 *
 * @author John Keyes (john at integralsource.com)
 * @version $Revision: 680644 $, $Date: 2008-07-29 01:13:48 -0700 (Tue, 29 Jul 2008) $
 */
public interface CommandLineParser
{
    /**
     * Parse the arguments according to the specified options.
     *
     * @param options the specified Options
     * @param arguments the command line arguments
     * @return the list of atomic option and value tokens
     *
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     */
    CommandLine parse(Options options, String[] arguments) throws ParseException;

    /**
     * Parse the arguments according to the specified options and
     * properties.
     *
     * @param options the specified Options
     * @param arguments the command line arguments
     * @param properties command line option name-value pairs
     * @return the list of atomic option and value tokens
     *
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     */
    /* To maintain binary compatibility, this is commented out.
       It is still in the abstract Parser class, so most users will
       still reap the benefit.
    CommandLine parse(Options options, String[] arguments, Properties properties)
               throws ParseException;
     */

    /**
     * Parse the arguments according to the specified options.
     *
     * @param options the specified Options
     * @param arguments the command line arguments
     * @param stopAtNonOption specifies whether to continue parsing the
     * arguments if a non option is encountered.
     *
     * @return the list of atomic option and value tokens
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     */
    CommandLine parse(Options options, String[] arguments, boolean stopAtNonOption) throws ParseException;

    /**
     * Parse the arguments according to the specified options and
     * properties.
     *
     * @param options the specified Options
     * @param arguments the command line arguments
     * @param properties command line option name-value pairs
     * @param stopAtNonOption specifies whether to continue parsing the
     *
     * @return the list of atomic option and value tokens
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     */
    /* To maintain binary compatibility, this is commented out.
       It is still in the abstract Parser class, so most users will
       still reap the benefit.
    CommandLine parse(Options options, String[] arguments, Properties properties, boolean stopAtNonOption)
            throws ParseException;
     */
}
