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
import java.util.LinkedList;
import java.util.List;

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int complete(final Session session, final CommandLine commandLine, final List candidates) {
        // buffer could be null
        assert candidates != null;

        List<Completion> completions = new ArrayList<Completion>(completers.size());

        // Run each completer, saving its completion results
        int max = -1;
        for (Completer completer : completers) {
            Completion completion = new Completion(candidates);
            completion.complete(session, completer, commandLine);

            // Compute the max cursor position
            if (completion.cursor > max) {
                completions.clear();
                completions.add(completion);
                max = completion.cursor;
            } else if (completion.cursor == max) {
                completions.add(completion);
            }
        }

        // Append candidates from completions which have the same cursor position as max
        for (Completion completion : completions) {
            // noinspection unchecked
            candidates.addAll(completion.candidates);
        }

        return max;
    }

    private class Completion
    {
        public final List<String> candidates;

        public int cursor;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Completion(final List candidates) {
            assert candidates != null;

            // noinspection unchecked
            this.candidates = new LinkedList<String>(candidates);
        }

        public void complete(final Session session, final Completer completer, final CommandLine commandLine) {
            assert completer != null;

            this.cursor = completer.complete(session, commandLine, candidates);
        }
    }
}
