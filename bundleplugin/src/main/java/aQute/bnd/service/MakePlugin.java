package aQute.bnd.service;

import java.util.*;

import aQute.lib.osgi.*;

public interface MakePlugin {

    /**
     * This plugin is called when Include-Resource detects a reference to a resource
     * that it can not find in the file system.
     * 
     * @param builder   The current builder
     * @param source    The source string (i.e. the place where bnd looked)
     * @param arguments Any arguments on the clause in Include-Resource
     * @return          A resource or null if no resource could be made
     * @throws Exception
     */
    Resource make(Builder builder, String source, Map<String,String> arguments) throws Exception;

}
