package com.github.nkzawa.socketio.androidchat;

import android.app.Application;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ChatApplication extends Application {


    private final TrustManager[] trustAllCerts= new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }
    } };
    
    private Socket mSocket;
    {
        try {
            // Configure options
            IO.Options options = new IO.Options();
// ... add more options

// End point https
            String yourEndpoint = Constants.CHAT_SERVER_URL;
            final String yourHostName = "192.168.1.205:3080";

// If https, explicitly tell set the sslContext.
            if (yourEndpoint.startsWith("https://")) {
                try {
                    // Default settings for all sockets

                    // Set default ssl context
                    IO.setDefaultSSLContext(SSLContext.getDefault());

                    // Set default hostname
                    HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            //HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                            //return hv.verify(yourHostName, session);
                            return true;
                        }
                    };
                    IO.setDefaultHostnameVerifier(hostnameVerifier);

                    SSLContext mySSLContext = SSLContext.getInstance("TLS");
                    mySSLContext.init(null, trustAllCerts, null);

                    // set as an option
                    options.sslContext = mySSLContext;
                    options.hostnameVerifier = hostnameVerifier;
                    options.secure = true;

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

// Instantiate the socket
            mSocket = IO.socket(Constants.CHAT_SERVER_URL, options);

            //mSocket = IO.socket(Constants.CHAT_SERVER_URL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

}
