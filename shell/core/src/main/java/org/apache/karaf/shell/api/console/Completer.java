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

import java.util.ArrayList;
import java.util.List;

/**
 * A <code>Completer</code> is used by the console to complete the command line.
 */
public interface Completer {

    /**
     * populate possible completion candidates.
     *
     * @param session the current {@link Session}
     * @param commandLine the pre-parsed {@link CommandLine}
     * @param candidates a list to fill with possible completion candidates
     * @return the index of the{@link CommandLine} for which the completion will be relative
     */
    int complete(Session session, CommandLine commandLine, List<String> candidates);

    default void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
        List<String> strings = new ArrayList<>();
        int idx = complete(session, commandLine, strings);
        String word = "";
        if (idx > commandLine.getBufferPosition() - commandLine.getArgumentPosition()) {
            word = commandLine.getBuffer().substring(commandLine.getBufferPosition() - commandLine.getArgumentPosition(), idx);
        }
        for (String string : strings) {
            String str = word + string;
            if (str.endsWith(" ")) {
                candidates.add(new Candidate(str.substring(0, str.length() - 1), true));
            } else {
                candidates.add(new Candidate(word + string, false));
            }
        }
    }
}
