package org.apache.karaf.tooling.assembly;

import java.util.List;

/**
 * Base class for Artifact Parser for each phase.
 */
class AbstractPhasedArtifactParser {

    protected String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

}
