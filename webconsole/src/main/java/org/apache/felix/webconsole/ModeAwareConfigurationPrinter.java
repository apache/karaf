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
package org.apache.felix.webconsole;


import java.io.PrintWriter;


/**
 * This is an optional extension of the {@link ConfigurationPrinter}.
 * If a configuration printer implements this interface, the
 * {@link #printConfiguration(PrintWriter, String)} method is used
 * for printing the configuration instead of the
 * {@link ConfigurationPrinter#printConfiguration(PrintWriter)}
 * method.
 *
 * A service implementing this method must still register itself
 * as a {@link ConfigurationPrinter} but not as a
 * {@link ModeAwareConfigurationPrinter} service.
 * @since 3.0
 */
public interface ModeAwareConfigurationPrinter
    extends ConfigurationPrinter
{

    /**
     * Prints the configuration report to the given <code>printWriter</code>.
     * Implementations are free to print whatever information they deem useful.
     * The <code>printWriter</code> may be flushed but must not be closed.
     * @param printWriter The print writer to use.
     * @param mode The rendering mode.
     */
    void printConfiguration( PrintWriter printWriter, String mode );
}
