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
package org.osgi.service.command;

import java.io.InputStream;
import java.io.PrintStream;

public interface CommandSession
{
    /**
     * Execute a program in this session.
     *
     * @param commandline
     * @return the result of the execution
     */
    Object execute(CharSequence commandline) throws Exception;

    /**
     * Close this command session. After the session is closed, it will throw
     * IllegalStateException when it is used.
     *
     * @param
     */
    void close();

    /**
     * Return the input stream that is the first of the pipeline. This stream is
     * sometimes necessary to communicate directly to the end user. For example,
     * a "less" or "more" command needs direct input from the keyboard to
     * control the paging.
     *
     * @return InpuStream used closest to the user or null if input is from a
     *         file.
     */
    InputStream getKeyboard();

    /**
     * Return the PrintStream for the console. This must always be the stream
     * "closest" to the user. This stream can be used to post messages that
     * bypass the piping. If the output is piped to a file, then the object
     * returned must be null.
     *
     * @return
     */
    PrintStream getConsole();

    /**
     * Get the value of a variable.
     *
     * @param name
     * @return
     */
    Object get(String name);

    /**
     * Set the value of a variable.
     *
     * @param name  Name of the variable.
     * @param value Value of the variable
     */
    void put(String name, Object value);

    /**
     * Convert an object to string form (CharSequence). The level is defined in
     * the Converter interface, it can be one of INSPECT, LINE, PART. This
     * function always returns a non null value. As a last resort, toString is
     * called on the Object.
     *
     * @param target
     * @param level
     * @return
     */
    CharSequence format(Object target, int level);

	/**
	 * Convert an object to another type.
	 */
	
	Object convert(Class<?> type, Object instance);
}
