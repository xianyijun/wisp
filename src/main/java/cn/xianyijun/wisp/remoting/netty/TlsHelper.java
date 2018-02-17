package cn.xianyijun.wisp.remoting.netty;


import cn.xianyijun.wisp.utils.StringUtils;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Properties;

import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_CLIENT_AUTHSERVER;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_CLIENT_CERTPATH;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_CLIENT_KEYPASSWORD;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_CLIENT_KEYPATH;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_CLIENT_TRUSTCERTPATH;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_SERVER_AUTHCLIENT;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_SERVER_CERTPATH;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_SERVER_KEYPASSWORD;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_SERVER_KEYPATH;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_SERVER_NEED_CLIENT_AUTH;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_SERVER_TRUSTCERTPATH;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_TEST_MODE_ENABLE;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsClientAuthServer;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsClientCertPath;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsClientKeyPassword;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsClientKeyPath;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsClientTrustCertPath;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsServerAuthClient;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsServerCertPath;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsServerKeyPassword;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsServerKeyPath;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsServerNeedClientAuth;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsServerTrustCertPath;
import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.tlsTestModeEnable;

/**
 * The type Tls helper.
 *
 * @author xianyijun
 */
@Slf4j
public class TlsHelper {

    /**
     * The interface Decryption strategy.
     */
    public interface DecryptionStrategy {
        /**
         * Decrypt private key input stream.
         *
         * @param privateKeyEncryptPath the private key encrypt path
         * @param forClient             the for client
         * @return input stream
         * @throws IOException the io exception
         */
        InputStream decryptPrivateKey(String privateKeyEncryptPath, boolean forClient) throws IOException;
    }

    private static DecryptionStrategy decryptionStrategy = (privateKeyEncryptPath, forClient) -> new FileInputStream(privateKeyEncryptPath);


    /**
     * Register decryption strategy.
     *
     * @param decryptionStrategy the decryption strategy
     */
    public static void registerDecryptionStrategy(final DecryptionStrategy decryptionStrategy) {
        TlsHelper.decryptionStrategy = decryptionStrategy;
    }

    /**
     * Build ssl context ssl context.
     *
     * @param forClient the for client
     * @return the ssl context
     * @throws IOException          the io exception
     * @throws CertificateException the certificate exception
     */
    public static SslContext buildSslContext(boolean forClient) throws IOException, CertificateException {
        File configFile = new File(TlsSystemConfig.tlsConfigFile);
        extractTlsConfigFromFile(configFile);
        logTheFinalUsedTlsConfig();

        SslProvider provider;
        if (OpenSsl.isAvailable()) {
            provider = SslProvider.OPENSSL;
            log.info("Using OpenSSL provider");
        } else {
            provider = SslProvider.JDK;
            log.info("Using JDK SSL provider");
        }

        if (forClient) {
            if (tlsTestModeEnable) {
                return SslContextBuilder
                        .forClient()
                        .sslProvider(SslProvider.JDK)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } else {
                SslContextBuilder sslContextBuilder = SslContextBuilder.forClient().sslProvider(SslProvider.JDK);


                if (!tlsClientAuthServer) {
                    sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                } else {
                    if (!StringUtils.isEmpty(tlsClientTrustCertPath)) {
                        sslContextBuilder.trustManager(new File(tlsClientTrustCertPath));
                    }
                }

                return sslContextBuilder.keyManager(
                        !StringUtils.isEmpty(tlsClientCertPath) ? new FileInputStream(tlsClientCertPath) : null,
                        !StringUtils.isEmpty(tlsClientKeyPath) ? decryptionStrategy.decryptPrivateKey(tlsClientKeyPath, true) : null,
                        !StringUtils.isEmpty(tlsClientKeyPassword) ? tlsClientKeyPassword : null)
                        .build();
            }
        } else {

            if (tlsTestModeEnable) {
                SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
                return SslContextBuilder
                        .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                        .sslProvider(SslProvider.JDK)
                        .clientAuth(ClientAuth.OPTIONAL)
                        .build();
            } else {
                SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(
                        !StringUtils.isEmpty(tlsServerCertPath) ? new FileInputStream(tlsServerCertPath) : null,
                        !StringUtils.isEmpty(tlsServerKeyPath) ? decryptionStrategy.decryptPrivateKey(tlsServerKeyPath, false) : null,
                        !StringUtils.isEmpty(tlsServerKeyPassword) ? tlsServerKeyPassword : null)
                        .sslProvider(provider);

                if (!tlsServerAuthClient) {
                    sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                } else {
                    if (!StringUtils.isEmpty(tlsServerTrustCertPath)) {
                        sslContextBuilder.trustManager(new File(tlsServerTrustCertPath));
                    }
                }

                sslContextBuilder.clientAuth(parseClientAuthMode(tlsServerNeedClientAuth));
                return sslContextBuilder.build();
            }
        }
    }

    private static void extractTlsConfigFromFile(final File configFile) {
        if (!(configFile.exists() && configFile.isFile() && configFile.canRead())) {
            log.info("Tls config file doesn't exist, skip it");
            return;
        }

        Properties properties;
        properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            properties.load(inputStream);
        } catch (IOException ignore) {
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                }
            }
        }

        tlsTestModeEnable = Boolean.parseBoolean(properties.getProperty(TLS_TEST_MODE_ENABLE, String.valueOf(tlsTestModeEnable)));
        tlsServerNeedClientAuth = properties.getProperty(TLS_SERVER_NEED_CLIENT_AUTH, tlsServerNeedClientAuth);
        tlsServerKeyPath = properties.getProperty(TLS_SERVER_KEYPATH, tlsServerKeyPath);
        tlsServerKeyPassword = properties.getProperty(TLS_SERVER_KEYPASSWORD, tlsServerKeyPassword);
        tlsServerCertPath = properties.getProperty(TLS_SERVER_CERTPATH, tlsServerCertPath);
        tlsServerAuthClient = Boolean.parseBoolean(properties.getProperty(TLS_SERVER_AUTHCLIENT, String.valueOf(tlsServerAuthClient)));
        tlsServerTrustCertPath = properties.getProperty(TLS_SERVER_TRUSTCERTPATH, tlsServerTrustCertPath);

        tlsClientKeyPath = properties.getProperty(TLS_CLIENT_KEYPATH, tlsClientKeyPath);
        tlsClientKeyPassword = properties.getProperty(TLS_CLIENT_KEYPASSWORD, tlsClientKeyPassword);
        tlsClientCertPath = properties.getProperty(TLS_CLIENT_CERTPATH, tlsClientCertPath);
        tlsClientAuthServer = Boolean.parseBoolean(properties.getProperty(TLS_CLIENT_AUTHSERVER, String.valueOf(tlsClientAuthServer)));
        tlsClientTrustCertPath = properties.getProperty(TLS_CLIENT_TRUSTCERTPATH, tlsClientTrustCertPath);
    }

    private static void logTheFinalUsedTlsConfig() {
        log.info("Log the final used tls related configuration");
        log.info("{} = {}", TLS_TEST_MODE_ENABLE, tlsTestModeEnable);
        log.info("{} = {}", TLS_SERVER_NEED_CLIENT_AUTH, tlsServerNeedClientAuth);
        log.info("{} = {}", TLS_SERVER_KEYPATH, tlsServerKeyPath);
        log.info("{} = {}", TLS_SERVER_KEYPASSWORD, tlsServerKeyPassword);
        log.info("{} = {}", TLS_SERVER_CERTPATH, tlsServerCertPath);
        log.info("{} = {}", TLS_SERVER_AUTHCLIENT, tlsServerAuthClient);
        log.info("{} = {}", TLS_SERVER_TRUSTCERTPATH, tlsServerTrustCertPath);

        log.info("{} = {}", TLS_CLIENT_KEYPATH, tlsClientKeyPath);
        log.info("{} = {}", TLS_CLIENT_KEYPASSWORD, tlsClientKeyPassword);
        log.info("{} = {}", TLS_CLIENT_CERTPATH, tlsClientCertPath);
        log.info("{} = {}", TLS_CLIENT_AUTHSERVER, tlsClientAuthServer);
        log.info("{} = {}", TLS_CLIENT_TRUSTCERTPATH, tlsClientTrustCertPath);
    }

    private static ClientAuth parseClientAuthMode(String authMode) {
        if (null == authMode || authMode.trim().isEmpty()) {
            return ClientAuth.NONE;
        }

        for (ClientAuth clientAuth : ClientAuth.values()) {
            if (clientAuth.name().equals(authMode.toUpperCase())) {
                return clientAuth;
            }
        }

        return ClientAuth.NONE;
    }

}

