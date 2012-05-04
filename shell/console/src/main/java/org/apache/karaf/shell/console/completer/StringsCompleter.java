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
package org.apache.karaf.shell.console.completer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.karaf.shell.console.Completer;

/**
 * Completer for a set of strings.
 */
public class StringsCompleter
    implements Completer
{
    private final SortedSet<String> strings = new TreeSet<String>();
    private final boolean caseSensitive;

    public StringsCompleter() {
        this(true);
    }


    public StringsCompleter(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public StringsCompleter(final Collection<String> strings) {
        this();
        assert strings != null;
        getStrings().addAll(strings);
    }

    public StringsCompleter(final String[] strings, boolean caseSensitive) {
        this(Arrays.asList(strings), caseSensitive);
    }

    public StringsCompleter(final Collection<String> strings, boolean caseSensitive) {
        this(caseSensitive);
        assert strings != null;
        getStrings().addAll(strings);
    }

    public StringsCompleter(final String[] strings) {
        this(Arrays.asList(strings));
    }

    public SortedSet<String> getStrings() {
        return strings;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int complete(String buffer, final int cursor, final List candidates) {
        // buffer could be null
        assert candidates != null;

        if (buffer == null) {
            buffer = "";
        }
        if (!caseSensitive) {
            buffer = buffer.toLowerCase();
        }

        // KARAF-421, use getStrings() instead strings field.
        SortedSet<String> matches = getStrings().tailSet(buffer);

        for (String match : matches) {
            String s = caseSensitive ? match : match.toLowerCase();
            if (!s.startsWith(buffer)) {
                break;
            }

            // noinspection unchecked
            candidates.add(match);
        }

        if (candidates.size() == 1) {
            // noinspection unchecked
            candidates.set(0, candidates.get(0) + " ");
        }

        return candidates.isEmpty() ? -1 : 0;
    }
}
