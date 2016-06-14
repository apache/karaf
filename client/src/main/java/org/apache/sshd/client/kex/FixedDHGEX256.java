package org.apache.sshd.client.kex;

import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sshd.common.KeyExchange;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.digest.SHA256;
import org.apache.sshd.common.kex.DH;
import org.apache.sshd.common.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedDHGEX256 extends FixedDHGEX {

    /**
     * Named factory for DHGEX key exchange
     */
    public static class Factory implements NamedFactory<KeyExchange> {

        public String getName() {
            return "diffie-hellman-group-exchange-sha256";
        }

        public KeyExchange create() {
            return new FixedDHGEX256();
        }

    }

    public FixedDHGEX256() {
    }

    @Override
    protected DH getDH(BigInteger p, BigInteger g) throws Exception {
        DH dh = new DH(new SHA256.Factory());
        dh.setP(p);
        dh.setG(g);
        return dh;
    }

}
