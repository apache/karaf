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
package org.apache.felix.service.threadio;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Enable multiplexing of the standard IO streams for input, output, and error.
 * <p/>
 * This service guards the central resource of IO streams. The standard streams
 * are singletons. This service replaces the singletons with special versions that
 * can find a unique stream for each thread. If no stream is associated with a
 * thread, it will use the standard input/output that was originally set.
 *
 * @author aqute
 */
public interface ThreadIO
{
    /**
     * Associate this streams with the current thread.
     * <p/>
     * Ensure that when output is performed on System.in, System.out, System.err it
     * will happen on the given streams.
     * <p/>
     * The streams will automatically be canceled when the bundle that has gotten
     * this service is stopped or returns this service.
     *
     * @param in  InputStream to use for the current thread when System.in is used
     * @param out PrintStream to use for the current thread when System.out is used
     * @param err PrintStream to use for the current thread when System.err is used
     */
    void setStreams(InputStream in, PrintStream out, PrintStream err);

    /**
     * Cancel the streams associated with the current thread.
     * <p/>
     * This method will not do anything when no streams are associated.
     */
    void close();
}
