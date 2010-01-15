package org.apache.felix.dm.samples.annotation;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.dm.annotation.api.Service;

/**
 * A French Dictionary Service.
 */
@Service(properties={"language=fr"})
public class FrenchDictionary implements DictionaryService
{
    private List<String> m_words = Arrays.asList("bonjour", "salut");
               
    /**
     * Check if a word exists if the list of words we have been configured from ConfigAdmin.
     */
    public boolean checkWord(String word)
    {
        return m_words.contains(word);
    }
}
