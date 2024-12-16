/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules.publickey;

import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.security.auth.Subject;
import javax.security.auth.login.FailedLoginException;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.NamePubkeyCallbackHandler;
import org.junit.Test;

public class PublicKeyLoginModuleTest {

    private static final String PK_PROPERTIES_FILE = "org/apache/karaf/jaas/modules/publickey/pubkey.properties";
    private static final String PK_USERS_FILE = "org/apache/karaf/jaas/modules/publickey/pubkey.users";

    private static final String RSA_KNOWN_MODULUS =
            "2504227846033126752625313329217708474924890377669312098933267135871562327792150810915433595733"
            + "979130785790337621243914845149325143098632580183245971502051291613503136182182218708721890923769091345704"
            + "119963221758691543226829294312457492456071842409242817598014777158790065648435489978774648853589909638928"
            + "448069481622573966178879417253888452317622624006445863588961367514293886664167742695648199055900918338245"
            + "701727653606086096756173044470526840851957391900922886984556493506186438991284463663361749451775578708454"
            + "0181594148839238901052763862484299588887844606103377160953183624788815045644521767391398467190125279747";

    @Test
    public void testRSALogin() throws Exception {
        Properties options = getLoginModuleOptions();
        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        // Generate a PublicKey using the known values
        BigInteger modulus = new BigInteger(RSA_KNOWN_MODULUS);
        BigInteger exponent = new BigInteger("65537");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("rsa", publicKey), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertFalse(subject.getPrincipals().isEmpty());
        assertThat("rsa", isIn(names(subject.getPrincipals(UserPrincipal.class))));

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testRSALoginWithGroups() throws Exception {
        // add groups
        Properties users = loadFile(PK_USERS_FILE);
        PublickeyBackingEngine pbe = new PublickeyBackingEngine(users);
        pbe.addRole("rsa", "r1");
        pbe.addGroup("rsa", "group1");
        pbe.addRole("rsa", ""); // should be ignored
        pbe.addGroupRole("group1", "r2");
        pbe.addGroupRole("group1", ""); // should be ignored
        pbe.addGroupRole("group1", "r3");

        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        // generate a PublicKey using the known values
        BigInteger modulus = new BigInteger(RSA_KNOWN_MODULUS);
        BigInteger exponent = new BigInteger("65537");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("rsa", publicKey), null, getLoginModuleOptions());

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(5, subject.getPrincipals().size());
        assertThat("rsa", isIn(names(subject.getPrincipals(UserPrincipal.class))));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("r1", "r2", "r3"));
    }

    @Test
    public void testDSALogin() throws Exception {
        Properties options = getLoginModuleOptions();
        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        String p = "1076175237625726563105954460741409330556298182412863930703571469202992312952487088821612089126846931217220"
            + "139938550642040962241586994856559462488140821681403960733982209827487135132210000913512532065787125116985685638"
            + "40437219296134522589816052156357553531846010339651017908589163855315552516201352809575855397";
        String q = "918380515194943729419256231914804453973955269349";
        String g = "5928865413019314795162062081939159959737363875586187627523617102819491716184351195073908492559564825805562"
            + "104476892066919492044841627907376461274343797017375757242038772707578284292374846844427026690399002493750530347"
            + "2378225083646830569532678306021077676137269211638266431262139218141967811197461432032698462";
        String y = "3780682190459260799543888842390974417268312111951424991203659597814001671832656608276823896973755971735795"
            + "130565245682634187551545737028902938478313465290457154458005480679650487421678748598551351730312164280338152996"
            + "0448119336850459047721615478019482431582683540283279032651976075781966545889409150149549269";

        // Generate a PublicKey using the known values
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        KeySpec publicKeySpec = new DSAPublicKeySpec(new BigInteger(y), new BigInteger(p), new BigInteger(q), new BigInteger(g));
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("dsa", publicKey), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertFalse(subject.getPrincipals().isEmpty());
        assertThat("dsa", isIn(names(subject.getPrincipals(UserPrincipal.class))));
        // We didn't configure any roles
        assertTrue(names(subject.getPrincipals(RolePrincipal.class)).isEmpty());

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testECLogin() throws Exception {
        Properties options = getLoginModuleOptions();
        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        String x = "-29742501866672735446035294501787338870744851402037490785638836399245997090445";
        String y = "-47637824304079393691947094099226900728731860400898598466261954347951527449659";

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
        ECPoint pubPoint = new ECPoint(new BigInteger(x), new BigInteger(y));
        KeySpec keySpec = new ECPublicKeySpec(pubPoint, ecParameters);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("ec", publicKey), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertFalse(subject.getPrincipals().isEmpty());
        assertThat("ec", isIn(names(subject.getPrincipals(UserPrincipal.class))));
        assertThat("ssh", isIn(names(subject.getPrincipals(RolePrincipal.class))));

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testUnknownUser() throws Exception {
        Properties options = getLoginModuleOptions();
        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        // Generate a PublicKey using the known values
        BigInteger modulus = new BigInteger(RSA_KNOWN_MODULUS);
        BigInteger exponent = new BigInteger("65537");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("unknown", publicKey), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        try {
            module.login();
            fail("Failure expected on an unknown user");
        } catch (FailedLoginException ex) {
            // expected
        }
    }

    @Test
    public void testUnknownKeyRSA() throws Exception {
        Properties options = getLoginModuleOptions();
        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        // Generate a PublicKey using the known values
        String known_modulus = RSA_KNOWN_MODULUS.substring(0, RSA_KNOWN_MODULUS.length() - 1) + "3";
        assertEquals(known_modulus.length(), RSA_KNOWN_MODULUS.length());
        assertTrue(known_modulus.charAt(RSA_KNOWN_MODULUS.length() - 1) !=
                RSA_KNOWN_MODULUS.charAt(RSA_KNOWN_MODULUS.length() - 1));

        BigInteger modulus = new BigInteger(known_modulus);
        BigInteger exponent = new BigInteger("65537");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("rsa", publicKey), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        try {
            module.login();
            fail("Failure expected on an unknown user");
        } catch (FailedLoginException ex) {
            // expected
        }
    }

    @Test
    public void testUnknownKeyDSA() throws Exception {
        Properties options = getLoginModuleOptions();
        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        String p = "1076175237625726563105954460741409330556298182412863930703571469202992312952487088821612089126846931217220"
            + "139938550642040962241586994856559462488140821681403960733982209827487135132210000913512532065787125116985685638"
            + "40437219296134522589816052156357553531846010339651017908589163855315552516201352809575855397";
        String q = "918380515194943729419256231914804453973955269349";
        String g = "5928865413019314795162062081939159959737363875586187627523617102819491716184351195073908492559564825805562"
            + "104476892066919492044841627907376461274343797017375757242038772707578284292374846844427026690399002493750530347"
            + "2378225083646830569532678306021077676137269211638266431262139218141967811197461432032698462";
        String y = "3780682190459260799543888842390974417268312111951424991203659597814001671832656608276823896973755971735795"
            + "130565245682634187551545737028902938478313465290457154458005480679650487421678748598551351730312164280338152996"
            + "0448119336850459047721615478019482431582683540283279032651976075781966545889409150149549267";

        // Generate a PublicKey using the known values
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        KeySpec publicKeySpec = new DSAPublicKeySpec(new BigInteger(y), new BigInteger(p), new BigInteger(q), new BigInteger(g));
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("dsa", publicKey), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        try {
            module.login();
            fail("Failure expected on an unknown user");
        } catch (FailedLoginException ex) {
            // expected
        }
    }

    @Test
    public void testUnknownKeyEC() throws Exception {
        Properties options = getLoginModuleOptions();
        PublickeyLoginModule module = new PublickeyLoginModule();
        Subject subject = new Subject();

        String x = "2145382594999641569030545431803328907795332312211583318014254232969998637145";
        String y = "52282205184471090919696434245736603165041352971927370430120381994413951213993";

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
        ECPoint pubPoint = new ECPoint(new BigInteger(x), new BigInteger(y));
        KeySpec keySpec = new ECPublicKeySpec(pubPoint, ecParameters);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        module.initialize(subject, new NamePubkeyCallbackHandler("ec", publicKey), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        try {
            module.login();
            fail("Failure expected on an unknown user");
        } catch (FailedLoginException ex) {
            // expected
        }
    }

    protected Properties getLoginModuleOptions() throws IOException {
        return loadFile(PK_PROPERTIES_FILE);
    }

    private Properties loadFile(String name) throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File file = new File(basedir + "/target/test-classes/" + name);
        return new Properties(file);
    }
}