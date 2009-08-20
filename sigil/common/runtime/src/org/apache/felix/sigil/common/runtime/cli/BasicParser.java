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
 * The class BasicParser provides a very simple implementation of
 * the {@link Parser#flatten(Options,String[],boolean) flatten} method.
 *
 * @author John Keyes (john at integralsource.com)
 * @version $Revision: 680644 $, $Date: 2008-07-29 01:13:48 -0700 (Tue, 29 Jul 2008) $
 */
public class BasicParser extends Parser
{
    /**
     * <p>A simple implementation of {@link Parser}'s abstract
     * {@link Parser#flatten(Options, String[], boolean) flatten} method.</p>
     *
     * <p><b>Note:</b> <code>options</code> and <code>stopAtNonOption</code>
     * are not used in this <code>flatten</code> method.</p>
     *
     * @param options The command line {@link Options}
     * @param arguments The command line arguments to be parsed
     * @param stopAtNonOption Specifies whether to stop flattening
     * when an non option is found.
     * @return The <code>arguments</code> String array.
     */
    protected String[] flatten(Options options, String[] arguments, boolean stopAtNonOption)
    {
        // just echo the arguments
        return arguments;
    }
}
