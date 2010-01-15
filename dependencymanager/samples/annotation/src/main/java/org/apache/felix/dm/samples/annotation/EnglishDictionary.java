package org.apache.felix.dm.samples.annotation;

import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Service;

/**
 * An English Dictionary Service. We'll be configured using OSGi Config Admin.
 */
@Service(properties={"language=en"})
public class EnglishDictionary implements DictionaryService
{
    private CopyOnWriteArrayList<String> m_words = new CopyOnWriteArrayList<String>();
    
    /**
     * Our service will be initialized from ConfigAdmin, so we define here a configuration dependency
     * (by default, our PID is our full class name).
     * @param config The configuration where we'll lookup our words list (key="words").
     */
    @ConfigurationDependency
    protected void updated(Dictionary<String, ?> config) {
        m_words.clear();
        List<String> words = (List<String>) config.get("words");
        for (String word : words) {
            m_words.add(word);
        }
    }
           
    /**
     * Check if a word exists if the list of words we have been configured from ConfigAdmin.
     */
    public boolean checkWord(String word)
    {
        return m_words.contains(word);
    }
}
