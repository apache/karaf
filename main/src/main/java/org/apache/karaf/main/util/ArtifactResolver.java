package org.apache.karaf.main.util;

import java.net.URI;

public interface ArtifactResolver {
    URI resolve(URI artifactUri);
}
