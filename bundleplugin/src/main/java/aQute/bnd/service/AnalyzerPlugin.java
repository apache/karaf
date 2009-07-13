package aQute.bnd.service;

import aQute.lib.osgi.*;

public interface AnalyzerPlugin {

    /**
     * This plugin is called after analysis. The plugin is free to modify the
     * jar and/or change the classpath information (see referred, contained).
     * This plugin is called after analysis of the JAR but before manifest
     * generation.
     * 
     * @param analyzer
     * @return true if the classpace has been modified so that the bundle
     *         classpath must be reanalyzed
     * @throws Exception
     */

    boolean analyzeJar(Analyzer analyzer) throws Exception;
}
