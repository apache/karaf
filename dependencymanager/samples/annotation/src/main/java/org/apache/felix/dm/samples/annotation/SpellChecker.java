package org.apache.felix.dm.samples.annotation;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.shell.Command;
import org.osgi.service.log.LogService;

/**
 * Felix "spellcheck" Shell Command, used to check correct word spelling.
 */
@Service
public class SpellChecker implements Command
{
    /**
     * We'll use the OSGi log service for logging. If no log service is available, then we'll use a NullObject.
     */
    @ServiceDependency(required = false)
    private LogService m_log;

    /**
     * We'll store all Dictionaries is a CopyOnWrite list, in order to avoid method synchronization.
     */
    private CopyOnWriteArrayList<DictionaryService> m_dictionaries = new CopyOnWriteArrayList<DictionaryService>();

    /**
     * Inject a dictionary into this service.
     * @param serviceProperties the dictionary OSGi service properties
     * @param dictionary the new dictionary
     */
    @ServiceDependency(removed = "removeDictionary")
    protected void addDictionary(Map<String, String> serviceProperties, DictionaryService dictionary)
    {
        m_log.log(LogService.LOG_INFO, "added dictionary: " + dictionary + " (language="
            + serviceProperties.get("language") + ")");
        m_dictionaries.add(dictionary);
    }

    /**
     * Remove a dictionary from our service.
     * @param dictionary
     */
    protected void removeDictionary(DictionaryService dictionary)
    {
        m_log.log(LogService.LOG_INFO, "added dictionary: " + dictionary);
        m_dictionaries.remove(dictionary);
    }

    // --- Felix Shell Command interface ---

    public String getName()
    {
        return "spellcheck";
    }

    public String getUsage()
    {
        return "spellcheck word";
    }

    public String getShortDescription()
    {
        return "Spell checker application using DependencyManager annotations";
    }

    public void execute(String commandLine, PrintStream out, PrintStream err)
    {
        String[] tokens = commandLine.split(" ");
        if (tokens == null || tokens.length < 2)
        {
            err.println("Invalid parameters: " + commandLine + ". Usage: " + getUsage());
            return;
        }
        String word = tokens[1];
        for (DictionaryService dictionary : m_dictionaries)
        {
            if (dictionary.checkWord(tokens[1]))
            {
                out.println("word " + word + " is correct");
                return;
            }
        }
        err.println("word " + word + " is incorrect");
    }
}
