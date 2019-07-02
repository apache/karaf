/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.jaas.modules.publickey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.security.auth.login.FailedLoginException;

import org.junit.Test;

public class PublicKeyEncodingTest {

    @Test
    public void testRSAKeys() throws FailedLoginException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Generated using: ssh-keygen -t rsa
        String storedKey = "AAAAB3NzaC1yc2EAAAADAQABAAABAQDGX4CpCL49sWHaIuDE4VbGkdTMhsDLV3b8MDZ37Llsx3kRBs/x7G3OhSvQPhI"
            + "jMNcbnUnCr+6O6poKjRcFI1Aj76TiSSYlvz9QbsWqc50ZwCuR39h6F9u8f9k62AV7IVA4aNVSJBFn2nOA00HOWvDDrU3ykG0cPeJcmP1l"
            + "PeOO9WJVG7dc37v3soZZniIH+uop/UFQ4Ga0zWy4xjggAy2rE2p0BYHchrJb43ovInh5cGgXx2vNVwURsAf0TAPJwn7GLNpMYr3IFbRC3"
            + "Tbe1wPdy9YM4rFlKL78o/dFbvUOH+Vd1BlYDofoxT4kHxod7W5wPALBr/Bm8CD2tR6OLLoD";
        String knownModulus = "2504227846033126752625313329217708474924890377669312098933267135871562327792150810915433595733"
            + "979130785790337621243914845149325143098632580183245971502051291613503136182182218708721890923769091345704"
            + "119963221758691543226829294312457492456071842409242817598014777158790065648435489978774648853589909638928"
            + "448069481622573966178879417253888452317622624006445863588961367514293886664167742695648199055900918338245"
            + "701727653606086096756173044470526840851957391900922886984556493506186438991284463663361749451775578708454"
            + "0181594148839238901052763862484299588887844606103377160953183624788815045644521767391398467190125279747";

        // Generate a PublicKey using the known values
        BigInteger modulus = new BigInteger(knownModulus);
        BigInteger exponent = new BigInteger("65537");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        assertTrue(PublickeyLoginModule.equals(publicKey, storedKey));

        // Make sure a different stored key does not work
        String differentKey = "AAAAB3NzaC1yc2EAAAADAQABAAABAQC9nIk6uBMouH2KhMZnnVhkEGC7ZdSOHZbCcmQSsvK3bl/Ly2yzvXNdqqRhlyv"
            + "Lv/Qjq0i4HnZsOUFAsfarYh8A0IP238AhTCoAeZf+ga+Mpm2uc+AOgDzwupfMYs6Zz81HWr1UsDr+LCOJkCC1/zzh5lub/Obif49j+nC1XX"
            + "0fT0AJ9BeGnR9HWg3m72SCUmWYMSYGwgfjNqTtqA9IHxCfEr29J8YO7HiJME3zwj0ok133RuZASEclTYXtJkKYvAzE6obhBPw7J6kqETJIH"
            + "0G0SkNjIm7cWThBalzyqcfydZ+0O+f/3LuSSp7EawaKu3g8mHkjt8b8ZxtjhgY0BZNV";
        assertFalse(PublickeyLoginModule.equals(publicKey, differentKey));
    }

    @Test
    public void testRSA1024() throws FailedLoginException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Generated using: ssh-keygen -t rsa -b 1024
        String storedKey = "AAAAB3NzaC1yc2EAAAADAQABAAAAgQCpvuUWHwGUbRtunzgNaiKo9varvw3rZ6xRwV37/tNsMcdE98T07zF3UiUzfm79vv"
            + "u6LrsMX6yzR3j1hSKxGtHoCuLO+wdx20Cxn+aqDsQsjTEgOE3SnqUhqX0aFRWs9GUo2sXRZooR7+5EKhSzFTmkgmx0b/FhlJQ2/Bdc9woZAw==";
        String knownModulus = "1191994723232881252194746074531692276628392720352218105656446277364105948933208899459090143"
            + "34485583082055798404847857986526198262831735131892900109314572095535330090724020090628526184947685186417937"
            + "713630451839747221181072495928766941603698696083904958230358940260930311021743608730447712164571127205526640899";

        // Generate a PublicKey using the known values
        BigInteger modulus = new BigInteger(knownModulus);
        BigInteger exponent = new BigInteger("65537");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        assertTrue(PublickeyLoginModule.equals(publicKey, storedKey));

        // Make sure a different stored key does not work
        String differentKey = "AAAAB3NzaC1yc2EAAAADAQABAAABAQC9nIk6uBMouH2KhMZnnVhkEGC7ZdSOHZbCcmQSsvK3bl/Ly2yzvXNdqqRhlyv"
            + "Lv/Qjq0i4HnZsOUFAsfarYh8A0IP238AhTCoAeZf+ga+Mpm2uc+AOgDzwupfMYs6Zz81HWr1UsDr+LCOJkCC1/zzh5lub/Obif49j+nC1XX"
            + "0fT0AJ9BeGnR9HWg3m72SCUmWYMSYGwgfjNqTtqA9IHxCfEr29J8YO7HiJME3zwj0ok133RuZASEclTYXtJkKYvAzE6obhBPw7J6kqETJIH"
            + "0G0SkNjIm7cWThBalzyqcfydZ+0O+f/3LuSSp7EawaKu3g8mHkjt8b8ZxtjhgY0BZNV";
        assertFalse(PublickeyLoginModule.equals(publicKey, differentKey));
    }

    @Test
    public void testDSA() throws FailedLoginException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Generated using: ssh-keygen -t dsa
        String storedKey = "AAAAB3NzaC1kc3MAAACBAJlAn/bPWpugKCLyoQpe8AbSZiIxdEJhl+VV8YEH6jfb9lLPA9JkQAf/lnG1Jx01UM65RRyKtnMAiB"
            + "pkhrPy3DbqJ4FgYBmc1Sdiufomilq6zSbE0esJEMyxEvSNDQLqIiUcSwVyJJj1vpV6ZPA6ihipTIaiSV+rmfKcS05i27UlAAAAFQCg3ZtIytPmG"
            + "ILQ7OEifIJvCSlS5QAAAIBUbgpjk7vSWVNICgKG6OrXeK0kJYRG6AaUZSiB2neoABMyGIHQ8dBCk+jtYqRMYyoc+OPi5q43VcDMxgzR/cHGjZi6"
            + "0w/I3M83072dAdaoi0cleL/V8NaH+SOvkkYkAG57OIa3ly9PVpPfeXRnbbjkz1EsrvXIelqb5enLhlIgXgAAAIA11rUkN/J3K7nw/BiolhpZR3M"
            + "VhWWIJFjJyU7ZC0yO8a+3AExuhTI6YQvsyvlY69KCwAwZsZvx9DryDE5xTfhzYa5kV4mM4AJSrE8/GtxLUVPZLwV6eoZLv1RIqP543ihZtoFyVm"
            + "MaTQFj45Qo8uAuVDjx5mpk/Rk1pYPUd0lc1Q==";
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

        assertTrue(PublickeyLoginModule.equals(publicKey, storedKey));

        // Make sure a different stored key does not work
        String differentKey = "AAAAB3NzaC1kc3MAAACBALE+qmsDN5lJYqQUmtrM7RI4vFcAQnla7Jp8Qy5ZUf63IFEA+tDzareKZOadwYbHOrIq3bDFCMH"
            + "HIVvFWJNhcJUBH8ZZnk7942Sxg6P5B3OQlCC6O4ADpe6NmwsxCpjpkyJizWTwvTspE6vV32VMa70UJlL1OtymgsWDef8ZQKqBAAAAFQCwiMFuOv"
            + "t6AZ1PgOwytbS1ra/FswAAAIAvf9b+K6eF6Mx3CnUVMHVldK4VybXjn/GwARH7BG8HJ8aGmMLvhk2qKGN5NatxgAc6IzRcwFbKvtniTTh06seuY"
            + "CwIvHs+7nldZ255D23as90jAqstkBGt5NmX5R/TgHQPwQILJpydaYUEf6f/KU6MZPANo8cbEi2hxgljWCQcwAAAAIEAh2S+0V+64AZy8+T03eMX"
            + "yBmt4xn8JPJzIHizF4VeUpTVwyA2EsiG9/YEWEGATj7mAcfAmLKl5rV1tQdXgUl2uxCDXw91c9PrYbfrHJjD1Oj6xHOjExDZI31Z8S6OKwo7df7"
            + "0GumGSDsg0nibs5rEwkkcT64AOMn1o4JvabsP200=";
        assertFalse(PublickeyLoginModule.equals(publicKey, differentKey));
    }

    @Test
    public void testEC521() throws FailedLoginException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidParameterSpecException {
        // ecdsa-sha2-nistp521 - generated using: ssh-keygen -t ecdsa -b 521
        String storedKey = "AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbmlzdHA1MjEAAACFBADLxNQ1lf99/8WlEg1nYbDL+qcWY4KSFZG+"
            + "2euZ1hAO9m1ly4ByuqLhuf1M3JPhhOCMIn9ihrPcxplG2zjpOnhaugDdwGJn+qcwkZSXVWoZOxpYUaQRdfnSd5wAKo9XOPqvc/L3BeHK"
            + "mPPygLH7eW2MEz2qOWe7Bby9duELK+9Zn3ebOQ==";
        String x = "273209377797440455675669012133614826094878213786507845287169633163915658072657502796285437529808606"
            + "0585712688028315849324172582722748448938768134500098005690";
        String y = "297320154107898594969162703371411878757449109919929193169657424280609259087338914952452468191452153"
            + "1633519626430088785609447337443826933969196755052278553401";

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp521r1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
        ECPoint pubPoint = new ECPoint(new BigInteger(x), new BigInteger(y));
        KeySpec keySpec = new ECPublicKeySpec(pubPoint, ecParameters);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        assertTrue(PublickeyLoginModule.equals(publicKey, storedKey));

        // Make sure a different stored key does not work
        String differentKey = "AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBL4+Vytknywh/XuOluxIqcHRoBsZHa12z+jpK"
            + "pwuGFlzlq3yatwC8DqUaywJjzSnoGKSge9GBjuFYwvHN17hq8U=";
        assertFalse(PublickeyLoginModule.equals(publicKey, differentKey));
    }

    @Test
    public void testEC256() throws FailedLoginException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidParameterSpecException {
        // ecdsa-sha2-nistp256 - generated using: ssh-keygen -t ecdsa
        String storedKey = "AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBL4+Vytknywh/XuOluxIqcHRoBsZHa12z+jpK"
            + "pwuGFlzlq3yatwC8DqUaywJjzSnoGKSge9GBjuFYwvHN17hq8U=";
        String x = "-29742501866672735446035294501787338870744851402037490785638836399245997090445";
        String y = "-47637824304079393691947094099226900728731860400898598466261954347951527449659";

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
        ECPoint pubPoint = new ECPoint(new BigInteger(x), new BigInteger(y));
        KeySpec keySpec = new ECPublicKeySpec(pubPoint, ecParameters);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        assertTrue(PublickeyLoginModule.equals(publicKey, storedKey));

        // Make sure a different stored key does not work
        String differentKey = "AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBDWwVudH/QYr/Nlkf/lZ0yMXVL+PvXBsGat8"
            + "+n07/Bw0xZGP5E8+x1wbkZVS6qx8XyPMI61NnCRLawB+UX3ZE/A=";
        assertFalse(PublickeyLoginModule.equals(publicKey, differentKey));
    }

    @Test
    public void testEC256_2() throws FailedLoginException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidParameterSpecException {
        // ecdsa-sha2-nistp256 - generated using: ssh-keygen -t ecdsa
        String storedKey = "AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBDWwVudH/QYr/Nlkf/lZ0yMXVL+PvXBsGat8"
            + "+n07/Bw0xZGP5E8+x1wbkZVS6qx8XyPMI61NnCRLawB+UX3ZE/A=";
        String x = "24284145843828879115537963613603143837878136357229118319568173718380870376500";
        String y = "-26429272137078923303974425138822683171929812869671300956629169158527526562832";

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
        ECPoint pubPoint = new ECPoint(new BigInteger(x), new BigInteger(y));
        KeySpec keySpec = new ECPublicKeySpec(pubPoint, ecParameters);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        assertTrue(PublickeyLoginModule.equals(publicKey, storedKey));

        // Make sure a different stored key does not work
        String differentKey = "AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBL4+Vytknywh/XuOluxIqcHRoBsZHa12z+jpK"
            + "pwuGFlzlq3yatwC8DqUaywJjzSnoGKSge9GBjuFYwvHN17hq8U=";
        assertFalse(PublickeyLoginModule.equals(publicKey, differentKey));
    }

}
