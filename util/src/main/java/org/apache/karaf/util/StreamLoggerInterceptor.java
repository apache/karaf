/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;

public class StreamLoggerInterceptor extends PrintStream {

    private Logger logger;
    private String logLevel;

    public StreamLoggerInterceptor(OutputStream stream, String loggerName, String logLevel) {
        super(stream, true);
        this.logger = LoggerFactory.getLogger(loggerName);
        this.logLevel = logLevel;
    }

    @Override
    public void print(String s) {
        log(s);
        super.print(s);
    }

    @Override
    public void println(String s) {
        log(s);
        super.println(s);
    }

    private void log(String s) {
        if (logLevel.equalsIgnoreCase("debug")) {
            logger.debug(s);
        } else if (logLevel.equalsIgnoreCase("error")) {
            logger.error(s);
        } else if (logLevel.equalsIgnoreCase("fatal")) {
            logger.error(s);
        } else if (logLevel.equalsIgnoreCase("info")) {
            logger.info(s);
        } else if (logLevel.equalsIgnoreCase("trace")) {
            logger.trace(s);
        } else if (logLevel.equalsIgnoreCase("warn")) {
            logger.warn(s);
        }
    }

}
