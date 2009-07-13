package aQute.bnd.service;

import aQute.lib.osgi.*;

public interface SignerPlugin {
    /**
     * Sign the current jar. The alias is the given certificate 
     * keystore.
     * 
     * @param builder   The current builder that contains the jar to sign
     * @param alias     The keystore certificate alias
     * @throws Exception When anything goes wrong
     */
    void sign(Builder builder, String alias) throws Exception;
}
