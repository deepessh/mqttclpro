/**
 * Utility class to read encrypted PEM files and generate a
 * SSL Socket Factory based on the provided certificates.
 * The original code is by Sharon Asher (link below). I have modified
 * it to use a newer version of the BouncyCastle Library (v1.52)
 *
 * Reference - https://gist.github.com/sharonbn/4104301"
 */
package in.dc297.mqttclpro.SSL;

import android.text.TextUtils;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SSLUtil {

    public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
                                                    final String password, final String p12ClientFile) {
        try {
            /**
             * Add BouncyCastle as a Security Provider
             */
            Security.addProvider(new BouncyCastleProvider());

            JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");
            PEMParser reader = null;
            TrustManagerFactory trustManagerFactory = null;
            /**
             * Load Certificate Authority (CA) certificate
             */
            if(!TextUtils.isEmpty(caCrtFile)) {
                reader = new PEMParser(new FileReader(caCrtFile));
                X509CertificateHolder caCertHolder = (X509CertificateHolder) reader.readObject();
                reader.close();

                X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);
                /**
                 * CA certificate is used to authenticate server
                 */
                KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                caKeyStore.load(null, null);
                caKeyStore.setCertificateEntry("ca-certificate", caCert);

                trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(caKeyStore);
            }
            else{//load default android cakeystore
                KeyStore caKeyStore = KeyStore.getInstance("AndroidCAStore");
                caKeyStore.load(null);
                trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(caKeyStore);
            }
            KeyManagerFactory keyManagerFactory = null;

            /**
             * Load client certificate
             */
            if(!TextUtils.isEmpty(p12ClientFile)){
                KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
                clientKeyStore.load(new FileInputStream(p12ClientFile),(password!=null && password.length()>0)?password.toCharArray(): new char[0]);
                keyManagerFactory = KeyManagerFactory.getInstance("X509");
                keyManagerFactory.init(clientKeyStore,(password!=null && password.length()>0)?password.toCharArray(): new char[0]);
            }
            else if(!TextUtils.isEmpty(crtFile)) {
                reader = new PEMParser(new FileReader(crtFile));
                X509CertificateHolder certHolder = (X509CertificateHolder) reader.readObject();
                reader.close();

                X509Certificate cert = certificateConverter.getCertificate(certHolder);
                /**
                 * Load client private key
                 */
                reader = new PEMParser(new FileReader(keyFile));
                Object keyObject = reader.readObject();
                reader.close();

                PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
                JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");

                KeyPair key;

                if (keyObject instanceof PEMEncryptedKeyPair) {
                    key = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
                } else {
                    key = keyConverter.getKeyPair((PEMKeyPair) keyObject);
                }
                /**
                 * Client key and certificates are sent to server so it can authenticate the client
                 */
                KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                clientKeyStore.load(null, null);
                clientKeyStore.setCertificateEntry("certificate", cert);
                clientKeyStore.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
                        new Certificate[]{cert});

                keyManagerFactory = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(clientKeyStore, password.toCharArray());
            }

            /**
             * Create SSL socket factory
             */
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(keyManagerFactory!=null?keyManagerFactory.getKeyManagers():null, trustManagerFactory.getTrustManagers(), null);

            /**
             * Return the newly created socket factory object
             */
            return context.getSocketFactory();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}