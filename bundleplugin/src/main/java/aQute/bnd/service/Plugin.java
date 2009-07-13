package aQute.bnd.service;

import java.util.*;

import aQute.libg.reporter.*;

/**
 * An optional interface for plugins. If a plugin implements this interface then
 * it can receive the reminaing attributes and directives given in its clause as
 * well as the reporter to use.
 * 
 */
public interface Plugin {
    /**
     * Give the plugin the remaining properties.
     * 
     * When a plugin is declared, the clause can contain extra properties.
     * All the properties and directives are given to the plugin to use.
     * 
     * @param map attributes and directives for this plugin's clause
     */
    void setProperties(Map<String,String> map);
    
    /**
     * Set the current reporter. This is called at init time. This plugin
     * should report all errors and warnings to this reporter.
     * 
     * @param processor
     */
    void setReporter(Reporter processor);
}
