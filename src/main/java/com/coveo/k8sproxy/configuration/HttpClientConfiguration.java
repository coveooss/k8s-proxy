/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.configuration;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfiguration
{
    @Bean
    public CloseableHttpClient httpClient()
    {
        return disableSslValidationOnHttpClientBuilder(HttpClientBuilder.create()).build();

    }

    private HttpClientBuilder disableSslValidationOnHttpClientBuilder(HttpClientBuilder builder)
    {
        SSLContext sslContext;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();
            builder.setSSLContext(sslContext);
            builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("Could not disable SSL on HttpClient builder.", e);
        }
        return builder;
    }
}
