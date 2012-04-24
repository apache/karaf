/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.console.completer;

import org.apache.karaf.shell.console.Completer;

import java.util.ArrayList;
import java.util.List;

/**
 * Useful base class for completion related test cases
 */
public abstract class CompleterTestSupport {
    protected List<String> complete(Completer completer, String buf) {
        List<String> candidates = new ArrayList<String>();
        completer.complete(buf, buf.length(), candidates);
        return candidates;
    }
}
