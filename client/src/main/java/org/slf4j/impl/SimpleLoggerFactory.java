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
package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;

/**
 * An implementation of {@link ILoggerFactory} which always returns
 * {@link SimpleLogger} instances.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class SimpleLoggerFactory implements ILoggerFactory {

  final static SimpleLoggerFactory INSTANCE = new SimpleLoggerFactory();

  Map<String, Logger> loggerMap;

  public SimpleLoggerFactory() {
    loggerMap = new HashMap<String, Logger>();
  }

  /**
   * Return an appropriate {@link SimpleLogger} instance by name.
   */
  public Logger getLogger(String name) {
    Logger slogger = null;
    // protect against concurrent access of the loggerMap
    synchronized (this) {
      slogger = (Logger) loggerMap.get(name);
      if (slogger == null) {
        slogger = new SimpleLogger(name);
        loggerMap.put(name, slogger);
      }
    }
    return slogger;
  }
}
