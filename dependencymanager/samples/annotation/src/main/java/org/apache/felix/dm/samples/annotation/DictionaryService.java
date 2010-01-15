package org.apache.felix.dm.samples.annotation;

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
    public boolean checkWord(String word);
}
