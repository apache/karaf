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
package org.apache.karaf.log.core.internal;

import org.junit.Test;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class LogServiceLog4j2ImplTest {

    private static final String ROOT_LOGGER = "log4j.rootLogger";

    private static final String LOGGER_PREFIX = "log4j2.logger.";

    private static final String NAME_SUFFIX = ".name";

    private static final String LEVEL_SUFFIX = ".level";

    @Test
    public void testLoggerNameWithNumbers() {
        final String name = "some_logger_name";
        final String logger = "org.ops4j.pax.web";
        final String level = "WARN";

        final Dictionary<String, Object> config = new Hashtable<>();
        config.put(ROOT_LOGGER, "INFO");
        config.put(LOGGER_PREFIX + name + NAME_SUFFIX, logger);
        config.put(LOGGER_PREFIX + name + LEVEL_SUFFIX, level);

        final LogServiceInternal logServiceInternal = new LogServiceLog4j2Impl(config);

        assertThat(logServiceInternal.getLevel(logger), hasEntry(logger, level));
    }

    @Test
    public void testSetLevelForLoggerNameWithNumbers() {
        final String logger = "org.ops4j.pax.web";
        final String level = "WARN";

        final Dictionary<String, Object> config = new Hashtable<>();
        config.put(ROOT_LOGGER, "INFO");
        final LogServiceInternal logServiceInternal = new LogServiceLog4j2Impl(config);

        logServiceInternal.setLevel(logger, level);

        assertThat(logServiceInternal.getLevel(logger), hasEntry(logger, level));
    }

    @Test
    public void testUpdateLevelForLoggerNameWithNumbers() {
        final String name = "some_logger_name";
        final String logger = "org.ops4j.pax.web";

        final Dictionary<String, Object> config = new Hashtable<>();
        config.put(ROOT_LOGGER, "INFO");
        config.put(LOGGER_PREFIX + name + NAME_SUFFIX, logger);
        config.put(LOGGER_PREFIX + name + LEVEL_SUFFIX, "WARN");
        final LogServiceInternal logServiceInternal = new LogServiceLog4j2Impl(config);

        final String newLevel = "TRACE";
        logServiceInternal.setLevel(logger, newLevel);

        assertThat(logServiceInternal.getLevel(logger), hasEntry(logger, newLevel));
    }
}