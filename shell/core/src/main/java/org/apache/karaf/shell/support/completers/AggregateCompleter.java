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

package org.apache.karaf.shell.support.completers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;


/**
 * Completer which contains multiple completers and aggregates them together.
 */
public class AggregateCompleter implements Completer
{
    private final Collection<Completer> completers;

    public AggregateCompleter(final Collection<Completer> completers) {
        assert completers != null;
        this.completers = completers;
    }

    public int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        List<Candidate> cands = new ArrayList<>();
        completeCandidates(session, commandLine, cands);
        for (Candidate cand : cands) {
            candidates.add(cand.value());
        }
        return 0;
    }

    @Override
    public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
        // buffer could be null
        assert candidates != null;
        for (Completer completer : completers) {
            completer.completeCandidates(session, commandLine, candidates);
        }
    }

}
