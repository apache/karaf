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
package org.apache.karaf.shell.api.console;

/**
 * Session history.
 */
public interface History {

    /**
     * First available index.
     *
     * @return first index in the history
     */
    int first();

    /**
     * Last available index.
     *
     * @return last index in the history
     */
    int last();

    /**
     * Command at the given index.
     * Indices can range from <code>first()</code> to <code>last()</code>.
     *
     * @param index the index in the history.
     * @return the command in the history at the given index.
     */
    CharSequence get(int index);

    /**
     * Clear the history.
     */
    void clear();

}
