/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.examples.spellcheckservice;


/**
 * A simple service interface that defines a spell check service. A spell check
 * service checks the spelling of all words in a given passage. A passage is any
 * number of words separated by a space character and the following punctuation
 * marks: comma, period, exclamation mark, question mark, semi-colon, and colon.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface SpellCheckService
{
    /**
     * Checks a given passage for spelling errors. A passage is any number of
     * words separated by a space and any of the following punctuation marks:
     * comma (,), period (.), exclamation mark (!), question mark (?),
     * semi-colon (;), and colon(:).
     * 
     * @param passage the passage to spell check.
     * @return An array of misspelled words or null if no words are misspelled.
     */
    public String[] check( String passage );
}
