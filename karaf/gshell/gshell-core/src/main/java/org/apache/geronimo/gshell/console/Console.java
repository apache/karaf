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

package org.apache.geronimo.gshell.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Provides an abstraction of a console.
 *
 * @version $Rev: 706594 $ $Date: 2008-10-21 14:22:21 +0200 (Tue, 21 Oct 2008) $
 */
public abstract class Console
    implements Runnable
{
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected boolean running = false;

    protected boolean breakOnNull = true;

    protected boolean autoTrim = true;

    protected boolean ignoreEmpty = true;

    protected Prompter prompter = new Prompter() {
        public String prompt() {
            return "> ";
        }
    };

    protected Executor executor;

    protected ErrorHandler errorHandler = new ErrorHandler() {
        public Result handleError(Throwable error) {
            return Result.STOP;
        }
    };

    public Console(final Executor executor) {
        assert executor != null;

        this.executor = executor;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(final boolean running) {
        this.running = running;
    }

    public boolean isBreakOnNull() {
        return breakOnNull;
    }

    public void setBreakOnNull(final boolean breakOnNull) {
        this.breakOnNull = breakOnNull;
    }

    public boolean isAutoTrim() {
        return autoTrim;
    }

    public void setAutoTrim(final boolean autoTrim) {
        this.autoTrim = autoTrim;
    }

    public boolean isIgnoreEmpty() {
        return ignoreEmpty;
    }

    public void setIgnoreEmpty(final boolean ignoreEmpty) {
        this.ignoreEmpty = ignoreEmpty;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(final ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Prompter getPrompter() {
        return prompter;
    }

    public void setPrompter(final Prompter prompter) {
        this.prompter = prompter;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }

    public void run() {
        log.debug("Running");

        running = true;

        while (running) {
            try {
                running = work();
            }
            catch (Throwable t) {
                // Don't use {} here so we get the throwable detail in the log stream
                log.debug("Work failed: " + t, t);

                if (errorHandler != null) {
                    ErrorHandler.Result result = errorHandler.handleError(t);

                    // Allow the error handler to request that the loop stop
                    if (result == ErrorHandler.Result.STOP) {
                        log.debug("Error handler requested STOP");
                        running = false;
                    }
                }
            }
        }

        log.debug("Finished");
    }

    protected boolean work() throws Exception {
        String line;

        try {
            line = readLine(prompter.prompt());
        } catch (Throwable t) {
            log.debug("Closing shell due to an exception while reading input: " + t, t);
            return false;
        }

        // Stop on null (maybe, else ignore)
        if (line == null) {
            return !breakOnNull;
        }

        log.debug("Read line: {}", line);

        // Log the line as HEX if trace is enabled
        if (log.isTraceEnabled()) {
            StringBuilder idx = new StringBuilder();
            StringBuilder hex = new StringBuilder();

            byte[] bytes = line.getBytes();
            for (byte b : bytes) {
                String h = Integer.toHexString(b);

                hex.append("x").append(h).append(" ");
                idx.append(" ").append((char)b).append("  ");
            }

            log.trace("HEX: {}", hex);
            log.trace("     {}", idx);
        }

        // Auto trim the line (maybe)
        if (autoTrim) {
            line = line.trim();
        }

        // Ingore empty lines (maybe)
        if (ignoreEmpty && line.length() == 0) {
            return true;
        }

        // Execute the line
        Executor.Result result = executor.execute(line);

        // Allow executor to request that the loop stop
        if (result == Executor.Result.STOP) {
            log.debug("Executor requested STOP");
            return false;
        }

        return true;
    }

    protected abstract String readLine(String prompt) throws IOException;

    //
    // Prompter
    //

    public static interface Prompter
    {
        String prompt();
    }

    //
    // Executor
    //

    public static interface Executor
    {
        enum Result {
            CONTINUE,
            STOP
        }

        Result execute(String line) throws Exception;
    }

    //
    // ErrorHandler
    //

    public static interface ErrorHandler
    {
        enum Result {
            CONTINUE,
            STOP
        }

        Result handleError(Throwable error);
    }
}
