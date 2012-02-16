package ca.spencerelliott.mercury;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NaiveSSLSocketFactory {

	private static SSLSocketFactory sslSocketFactory;

    public static final SSLSocketFactory getSocketFactory()
    {
        if ( sslSocketFactory == null ) {
            try {
                TrustManager[] tm = new TrustManager[] { new NaiveTrustManager() };
                SSLContext context = SSLContext.getInstance ("TLS");
                context.init( new KeyManager[0], tm, new SecureRandom( ) );

                sslSocketFactory = (SSLSocketFactory) context.getSocketFactory ();

            } catch (KeyManagementException e) {
                //log.error ("No SSL algorithm support: " + e.getMessage(), e); 
            } catch (NoSuchAlgorithmException e) {
                //log.error ("Exception when setting up the Naive key management.", e);
            }
        }
        return sslSocketFactory;
    }

    static class NaiveTrustManager implements X509TrustManager {
        public void checkClientTrusted ( X509Certificate[] cert, String authType )
            throws CertificateException 
        {
        }

        public void checkServerTrusted ( X509Certificate[] cert, String authType ) 
            throws CertificateException 
        {
        }

        public X509Certificate[] getAcceptedIssuers ()
        {
            return null; 
        }
    }
}
