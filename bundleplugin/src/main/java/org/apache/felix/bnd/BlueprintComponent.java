package org.apache.felix.bnd;

import java.util.List;
import java.util.ArrayList;

import aQute.lib.spring.XMLTypeProcessor;
import aQute.lib.spring.XMLType;
import aQute.lib.osgi.Analyzer;

public class BlueprintComponent extends XMLTypeProcessor {

    protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
        List<XMLType> types = new ArrayList<XMLType>();

        String header = analyzer.getProperty("Bundle-Blueprint", "OSGI-INF/blueprint");
        process(types,"blueprint.xsl", header, ".*\\.xml"); 

        return types;
    }

}
