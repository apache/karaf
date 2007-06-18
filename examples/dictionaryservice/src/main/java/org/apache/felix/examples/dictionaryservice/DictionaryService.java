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
package org.apache.felix.examples.dictionaryservice;


/**
 * A simple service interface that defines a dictionary service. A dictionary
 * service simply verifies the existence of a word.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface DictionaryService
{
    /**
     * Check for the existence of a word.
     * 
     * @param word the word to be checked.
     * @return true if the word is in the dictionary, false otherwise.
     */
    public boolean checkWord( String word );

}
