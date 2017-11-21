/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.token;

import static com.coveo.k8sproxy.proxy.K8sReverseProxy.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.coveo.k8sproxy.domain.CsrfToken;
import com.coveo.k8sproxy.domain.JweToken;
import com.coveo.k8sproxy.domain.JweTokenRequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JweTokenRetriever
{
    @Value("${k8s.clusterEndpoint}")
    private String k8sClusterEndpoint;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CloseableHttpClient httpClient;

    public JweToken fetchJweToken(String googleIdToken) throws IOException
    {
        CsrfToken csrfToken;
        HttpGet csrfRequest = new HttpGet(k8sClusterEndpoint
                + "/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/api/v1/csrftoken/login");
        csrfRequest.setHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + googleIdToken);
        try (CloseableHttpResponse csrfResponse = httpClient.execute(csrfRequest)) {
            csrfToken = objectMapper.readValue(csrfResponse.getEntity().getContent(), CsrfToken.class);
        }

        HttpPost jweRequest = new HttpPost(k8sClusterEndpoint
                + "/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/api/v1/login");
        jweRequest.setHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + googleIdToken);
        jweRequest.setHeader("x-csrf-token", csrfToken.getToken());
        jweRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        jweRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(new JweTokenRequestBody().withToken(googleIdToken))));
        try (CloseableHttpResponse jweResponse = httpClient.execute(jweRequest)) {
            JweToken jweToken = objectMapper.readValue(IOUtils.toString(jweResponse.getEntity().getContent(),
                                                                        StandardCharsets.UTF_8),
                                                       JweToken.class);
            jweToken.setEncodedJweToken(URLEncoder.encode(jweToken.getJweToken(), StandardCharsets.UTF_8.toString()));
            return jweToken;
        }
    }
}
