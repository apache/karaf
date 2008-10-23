package org.apache.servicemix.kernel.gshell.core.remote;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.apache.geronimo.gshell.whisper.ssl.SSLContextFactory;
import org.apache.servicemix.kernel.jaas.config.KeystoreManager;

public class KeystoreSSLContextFactory implements SSLContextFactory {

    private KeystoreManager keystoreManager;
    private String clientProvider;
    private String clientProtocol = "TLS";
    private String clientAlgorithm = "SunX509";
    private String clientKeystore;
    private String clientKeyAlias;
    private String clientTruststore;
    private String serverProvider;
    private String serverProtocol = "TLS";
    private String serverAlgorithm = "SunX509";
    private String serverKeystore;
    private String serverKeyAlias;
    private String serverTruststore;

    public KeystoreManager getKeystoreManager() {
        return keystoreManager;
    }

    public void setKeystoreManager(KeystoreManager keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    public String getClientProvider() {
        return clientProvider;
    }

    public void setClientProvider(String clientProvider) {
        this.clientProvider = clientProvider;
    }

    public String getClientProtocol() {
        return clientProtocol;
    }

    public void setClientProtocol(String clientProtocol) {
        this.clientProtocol = clientProtocol;
    }

    public String getClientAlgorithm() {
        return clientAlgorithm;
    }

    public void setClientAlgorithm(String clientAlgorithm) {
        this.clientAlgorithm = clientAlgorithm;
    }

    public String getClientKeystore() {
        return clientKeystore;
    }

    public void setClientKeystore(String clientKeystore) {
        this.clientKeystore = clientKeystore;
    }

    public String getClientKeyAlias() {
        return clientKeyAlias;
    }

    public void setClientKeyAlias(String clientKeyAlias) {
        this.clientKeyAlias = clientKeyAlias;
    }

    public String getClientTruststore() {
        return clientTruststore;
    }

    public void setClientTruststore(String clientTruststore) {
        this.clientTruststore = clientTruststore;
    }

    public String getServerProvider() {
        return serverProvider;
    }

    public void setServerProvider(String serverProvider) {
        this.serverProvider = serverProvider;
    }

    public String getServerProtocol() {
        return serverProtocol;
    }

    public void setServerProtocol(String serverProtocol) {
        this.serverProtocol = serverProtocol;
    }

    public String getServerAlgorithm() {
        return serverAlgorithm;
    }

    public void setServerAlgorithm(String serverAlgorithm) {
        this.serverAlgorithm = serverAlgorithm;
    }

    public String getServerKeystore() {
        return serverKeystore;
    }

    public void setServerKeystore(String serverKeystore) {
        this.serverKeystore = serverKeystore;
    }

    public String getServerKeyAlias() {
        return serverKeyAlias;
    }

    public void setServerKeyAlias(String serverKeyAlias) {
        this.serverKeyAlias = serverKeyAlias;
    }

    public String getServerTruststore() {
        return serverTruststore;
    }

    public void setServerTruststore(String serverTruststore) {
        this.serverTruststore = serverTruststore;
    }

    public SSLContext createClientContext() throws GeneralSecurityException {
        return keystoreManager.createSSLContext(clientProvider, clientProtocol, clientAlgorithm,
                                                clientKeystore, clientKeyAlias, clientTruststore);
    }

    public SSLContext createServerContext() throws GeneralSecurityException {
        return keystoreManager.createSSLContext(serverProvider, serverProtocol, serverAlgorithm,
                                                serverKeystore, serverKeyAlias, serverTruststore);
    }

}
