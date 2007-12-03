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
package org.apache.geronimo.gshell.spring;

import org.apache.geronimo.gshell.command.IO;
import org.springframework.aop.TargetSource;

/**
 * A TargetSource that provides an IO that has to be 
 * previously set in a thread local storage.
 */
public class IOTargetSource implements TargetSource {

    private static ThreadLocal<IO> tls = new ThreadLocal<IO>();

    public static void setIO(IO io) {
        tls.set(io);
    }

    public Class getTargetClass() {
        return IO.class;
    }

    public boolean isStatic() {
        return false;
    }

    public Object getTarget() throws Exception {
        return tls.get();
    }

    public void releaseTarget(Object o) throws Exception {
    }
}
