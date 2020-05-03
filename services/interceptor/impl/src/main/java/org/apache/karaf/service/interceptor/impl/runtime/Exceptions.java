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
package org.apache.karaf.service.interceptor.impl.runtime;

import java.lang.reflect.InvocationTargetException;

public final class Exceptions {
    private Exceptions() {
        // no-op
    }

    public static Object unwrap(final InvocationTargetException ite) throws Exception {
        final Throwable targetException = ite.getTargetException();
        if (Exception.class.isInstance(targetException)) {
            throw Exception.class.cast(targetException);
        }
        if (Error.class.isInstance(targetException)) {
            throw Error.class.cast(targetException);
        }
        throw ite; // quite unlikely
    }
}
