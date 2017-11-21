/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.coveo.k8sproxy.domain.GoogleIdAndRefreshToken;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GoogleTokenRetriever
{
    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenRetriever.class);

    private String clientId;

    @Value("${google.clientSecret}")
    private String clientSecret;
    @Value("${google.tokenUrl}")
    private String tokenUrl;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CloseableHttpClient httpClient;

    private String authorizeUrl;

    @Autowired
    public GoogleTokenRetriever(@Value("${server.port}") String port,
                                @Value("${google.authorizeUrl}") String baseAuthorizeUrl,
                                @Value("${google.clientId}") String clientId)
    {
        this.authorizeUrl = baseAuthorizeUrl + "?redirect_uri=http://localhost:" + port
                + "/redirect_uri&response_type=code&client_id=" + clientId
                + "&scope=openid+email+profile&approval_prompt=force&access_type=offline";
        this.clientId = clientId;
    }

    public String getAuthorizeUrl()
    {
        return authorizeUrl;
    }

    public GoogleIdAndRefreshToken postForRefreshAndAccessToken(String code, String redirectUri) throws IOException
    {
        HttpPost callbackRequest = new HttpPost(tokenUrl);

        List<NameValuePair> parameters = new ArrayList<>();
        parameters.addAll(getAuthenticationParameters());
        parameters.addAll(Arrays.asList(new BasicNameValuePair("grant_type", "authorization_code"),
                                        new BasicNameValuePair("code", code),
                                        new BasicNameValuePair("redirect_uri", redirectUri)));
        callbackRequest.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));

        try (CloseableHttpResponse callbackResponse = httpClient.execute(callbackRequest)) {
            GoogleIdAndRefreshToken googleToken = objectMapper.readValue(IOUtils.toString(callbackResponse.getEntity()
                                                                                                          .getContent(),
                                                                                          StandardCharsets.UTF_8),
                                                                         GoogleIdAndRefreshToken.class);
            logger.info("New id token retrieved.");
            return googleToken;
        }
    }

    public GoogleIdAndRefreshToken refreshToken(String refreshToken) throws IOException
    {
        logger.info("Refreshing the google id token.");

        HttpPost refreshTokenRequest = new HttpPost(tokenUrl);

        List<NameValuePair> parameters = new ArrayList<>();
        parameters.addAll(getAuthenticationParameters());
        parameters.add(new BasicNameValuePair("grant_type", "refresh_token"));
        parameters.add(new BasicNameValuePair("refresh_token", refreshToken));
        refreshTokenRequest.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));

        try (CloseableHttpResponse refreshTokenResponse = httpClient.execute(refreshTokenRequest)) {
            GoogleIdAndRefreshToken googleIdToken = objectMapper.readValue(IOUtils.toString(refreshTokenResponse.getEntity()
                                                                                                                .getContent(),
                                                                                            StandardCharsets.UTF_8),
                                                                           GoogleIdAndRefreshToken.class);
            logger.info("New id token retrieved based on the refresh token.");
            return googleIdToken;
        }
    }

    private Collection<? extends NameValuePair> getAuthenticationParameters()
    {
        return Arrays.asList(new BasicNameValuePair("client_id", clientId),
                             new BasicNameValuePair("client_secret", clientSecret));
    }
}
