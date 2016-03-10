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
package org.apache.karaf.shell.api.console;

import java.util.EnumSet;

/**
 * Session terminal.
 */
public interface Terminal {

    /**
     * The infocmp type of this terminal
     *
     * @return the terminal type.
     */
    String getType();

    /**
     * Width of the terminal.
     *
     * @return the terminal width.
     */
    int getWidth();

    /**
     * Height of the terminal.
     *
     * @return the terminal height.
     */
    int getHeight();

    /**
     * Whether ansi sequences are supported or not.
     *
     * @return true if ANSI is supported, false else.
     */
    boolean isAnsiSupported();

    /**
     * Whether echo is enabled or not.
     *
     * @return true if echo is enabled, false else.
     */
    boolean isEchoEnabled();

    /**
     * Enable or disable echo.
     *
     * @param enabled true to enable echo, false else.
     */
    void setEchoEnabled(boolean enabled);

    /**
     * Add a qualified listener for the specific signal.
     *
     * @param listener the listener to register.
     * @param signal the signal the listener is interested in.
     */
    void addSignalListener(SignalListener listener, Signal... signal);

    /**
     * Add a qualified listener for the specific set of signal.
     *
     * @param listener the listener to register.
     * @param signals the signals the listener is interested in.
     */
    void addSignalListener(SignalListener listener, EnumSet<Signal> signals);

    /**
     * Add a global listener for all signals.
     *
     * @param listener the listener to register.
     */
    void addSignalListener(SignalListener listener);

    /**
     * Remove a previously registered listener for all the signals it was registered.
     *
     * @param listener the listener to remove.
     */
    void removeSignalListener(SignalListener listener);

}
