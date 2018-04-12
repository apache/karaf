/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.support.parsing;

import org.apache.karaf.shell.api.console.CommandLine;

/**
 *  The result of a delimited buffer.
 */
public class CommandLineImpl implements CommandLine {

    private final String[] arguments;
    private final int cursorArgumentIndex;
    private final int argumentPosition;
    private final int bufferPosition;
    private final String buffer;

    /**
     *  @param  arguments           the array of tokens
     *  @param  cursorArgumentIndex the token index of the cursor
     *  @param  argumentPosition    the position of the cursor in the
     *                              current token
     *  @param  bufferPosition      the position of the cursor in the whole buffer
     *  @param buffer               the whole buffer
     */
    public CommandLineImpl(String[] arguments, int cursorArgumentIndex, int argumentPosition, int bufferPosition, String buffer) {
        this.arguments = arguments;
        this.cursorArgumentIndex = cursorArgumentIndex;
        this.argumentPosition = argumentPosition;
        this.bufferPosition = bufferPosition;
        this.buffer = buffer;
    }

    public int getCursorArgumentIndex() {
        return this.cursorArgumentIndex;
    }

    public String getCursorArgument() {
        if ((cursorArgumentIndex < 0)
            || (cursorArgumentIndex >= arguments.length)) {
            return null;
        }

        return arguments[cursorArgumentIndex];
    }

    public int getArgumentPosition() {
        return this.argumentPosition;
    }

    public String[] getArguments() {
        return this.arguments;
    }

    public int getBufferPosition() {
        return this.bufferPosition;
    }

    public String getBuffer() {
        return this.buffer;
    }
}
