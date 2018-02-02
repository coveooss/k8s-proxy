/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.proxy;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.coveo.k8sproxy.domain.ClusterEndpoint;
import com.coveo.k8sproxy.domain.GoogleIdAndRefreshToken;
import com.coveo.k8sproxy.domain.JweToken;
import com.coveo.k8sproxy.domain.TokenInfo;
import com.coveo.k8sproxy.domain.exception.InvalidParameterException;
import com.coveo.k8sproxy.domain.exception.MissingParameterException;
import com.coveo.k8sproxy.token.GoogleTokenRetriever;
import com.coveo.k8sproxy.token.JweTokenRetriever;

@Controller
public class K8sReverseProxy implements DisposableBean
{
    private static final Logger logger = LoggerFactory.getLogger(K8sReverseProxy.class);

    public static final String BEARER_PREFIX = "Bearer ";
    private static final String REFRESH_TOKEN_FILENAME = "refresh_token";
    private static final String JWE_TOKEN = "jweToken";
    private static final int DEFAULT_REFRESH_TASK_FREQUENCY_IN_SECONDS = 60;

    @Value("${k8s.clusterEndpoint}")
    private String k8sClusterEndpoint;
    @Autowired
    private CloseableHttpClient httpClient;
    @Autowired
    private GoogleTokenRetriever googleTokenRetriever;
    @Autowired
    private JweTokenRetriever jweTokenRetriever;

    private GoogleIdAndRefreshToken googleToken;
    private JweToken jweToken;
    private String initialRedirect;

    private Timer timer = new Timer();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @RequestMapping(value = "/k8s_cluster_endpoint", method = RequestMethod.GET)
    @ResponseBody
    public ClusterEndpoint getCurrentEndpoint(HttpServletRequest request, HttpServletResponse response)
    {
        return new ClusterEndpoint(k8sClusterEndpoint);
    }

    @RequestMapping(value = "/k8s_cluster_endpoint/set", method = RequestMethod.GET)
    @ResponseBody
    public ClusterEndpoint setCurentEndpoint(@RequestParam String endpoint,
                                             HttpServletRequest request,
                                             HttpServletResponse response)
            throws MissingParameterException,
                InvalidParameterException,
                IOException
    {
        if (endpoint == null) {
            throw new MissingParameterException("endpoint");
        }

        try {
            new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new InvalidParameterException("endpoint", "is not a valid URL.");
        }

        logger.info("Setting cluster endpoint to value '{}'", endpoint);
        k8sClusterEndpoint = endpoint;

        WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            jweTokenRetriever.setK8sClusterEndpoint(k8sClusterEndpoint);
            jweToken = jweTokenRetriever.fetchJweToken(googleToken.getIdToken());
        } finally {
            writeLock.unlock();
        }

        return new ClusterEndpoint(k8sClusterEndpoint);
    }

    @RequestMapping("/redirect_uri")
    public void callback(@RequestParam String code, HttpServletRequest request, HttpServletResponse response)
            throws ClientProtocolException,
                IOException
    {
        WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            googleToken = googleTokenRetriever.postForRefreshAndAccessToken(code, request.getRequestURL().toString());

            jweToken = jweTokenRetriever.fetchJweToken(googleToken.getIdToken());

            try (PrintWriter out = new PrintWriter(REFRESH_TOKEN_FILENAME)) {
                out.print(googleToken.getRefreshToken());
            }
        } finally {
            writeLock.unlock();
        }

        scheduleRefreshTask(googleToken.getExpiresIn());

        response.sendRedirect(initialRedirect);
    }

    @RequestMapping("/get_token")
    @ResponseBody
    public TokenInfo getTokens(HttpServletRequest request, HttpServletResponse response)
            throws ClientProtocolException,
                IOException
    {
        if (googleToken == null || googleToken.getIdToken() == null || googleToken.getRefreshToken() == null) {
            initialRedirect = request.getRequestURI().toString();
            response.sendRedirect(googleTokenRetriever.getAuthorizeUrl());
            return null;
        }

        ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            return new TokenInfo().withIdToken(googleToken.getIdToken())
                                  .withRefreshToken(googleToken.getRefreshToken());
        } finally {
            readLock.unlock();
        }
    }

    @RequestMapping("/ui")
    public void uiRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendRedirect(request.getRequestURL().substring(0, request.getRequestURL().length() - 3)
                + "/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/");
    }

    @RequestMapping("/**")
    public void reverseProxy(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (googleToken == null || jweToken == null || jweToken.getJweToken() == null
                || jweToken.getEncodedJweToken() == null) {
            logger.info("Redirecting to Google for authentication.");
            initialRedirect = request.getRequestURI().toString();
            response.sendRedirect(googleTokenRetriever.getAuthorizeUrl());
            return;
        }

        InputStreamEntity inputStreamEntity = getInputStreamEntity(request);
        HttpUriRequest proxiedRequest = buildHttpRequest(request.getMethod(),
                                                         k8sClusterEndpoint + request.getRequestURI(),
                                                         inputStreamEntity,
                                                         request);

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)
                    && !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
                proxiedRequest.setHeader(headerName, request.getHeader(headerName));
            }

        }

        ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            proxiedRequest.setHeader(JWE_TOKEN, jweToken.getJweToken());
            proxiedRequest.setHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + googleToken.getIdToken());
            proxiedRequest.setHeader(HttpHeaders.COOKIE, jweToken.getEncodedJweToken());
        } finally {
            readLock.unlock();
        }

        try (CloseableHttpResponse proxiedResponse = httpClient.execute(proxiedRequest)) {
            response.setStatus(proxiedResponse.getStatusLine().getStatusCode());
            Stream.of(proxiedResponse.getAllHeaders())
                  .forEach(header -> response.setHeader(header.getName(), header.getValue()));
            if (proxiedResponse.getEntity() != null) {
                IOUtils.copy(proxiedResponse.getEntity().getContent(), response.getOutputStream());
            }
        }
    }

    @PostConstruct
    public void initialize()
    {
        try {
            String persistedRefreshToken = new String(Files.readAllBytes(Paths.get(REFRESH_TOKEN_FILENAME)),
                                                      StandardCharsets.UTF_8);
            persistedRefreshToken.replaceAll(System.getProperty("line.separator"), "");

            logger.info("Found persisted refresh token in the configuration file, using it to gain a new id token.");
            GoogleIdAndRefreshToken googleIdToken = googleTokenRetriever.refreshToken(persistedRefreshToken);
            googleToken = new GoogleIdAndRefreshToken().withRefreshToken(persistedRefreshToken)
                                                       .withIdToken(googleIdToken.getIdToken())
                                                       .withExpiresIn(googleIdToken.getExpiresIn());
            jweToken = jweTokenRetriever.fetchJweToken(googleIdToken.getIdToken());

            scheduleRefreshTask(googleIdToken.getExpiresIn());
        } catch (NoSuchFileException e) {
            // File not present, skipping
        } catch (IOException e) {
            logger.error("Error while trying to fetch the persisted refresh token.", e);
        }
    }

    private InputStreamEntity getInputStreamEntity(HttpServletRequest request) throws IOException
    {
        int contentLength = request.getContentLength();

        ContentType contentType = null;
        if (request.getContentType() != null) {
            contentType = ContentType.parse(request.getContentType());
        }

        return new InputStreamEntity(request.getInputStream(), contentLength, contentType);
    }

    private HttpUriRequest buildHttpRequest(String verb,
                                            String uri,
                                            InputStreamEntity entity,
                                            HttpServletRequest request)
    {
        HttpUriRequest httpRequest;
        String uriWithQueryString = uri + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        switch (verb.toUpperCase()) {
            case "POST":
                HttpPost httpPost = new HttpPost(uriWithQueryString);
                httpRequest = httpPost;
                httpPost.setEntity(entity);
                break;
            case "PUT":
                HttpPut httpPut = new HttpPut(uriWithQueryString);
                httpRequest = httpPut;
                httpPut.setEntity(entity);
                break;
            case "PATCH":
                HttpPatch httpPatch = new HttpPatch(uriWithQueryString);
                httpRequest = httpPatch;
                httpPatch.setEntity(entity);
                break;
            case "DELETE":
                HttpDeleteWithEntity entityRequest = new HttpDeleteWithEntity(uriWithQueryString);
                httpRequest = entityRequest;
                entityRequest.setEntity(entity);
                break;
            default:
                httpRequest = new HttpGet(uriWithQueryString);
        }
        return httpRequest;
    }

    private void scheduleRefreshTask(long tokenValidityInSeconds)
    {
        timer.schedule(new TokenRefreshTask(),
                       new Date(Instant.now()
                                       .plus(tokenValidityInSeconds != 0 ? tokenValidityInSeconds / 2
                                                                         : DEFAULT_REFRESH_TASK_FREQUENCY_IN_SECONDS,
                                             ChronoUnit.SECONDS)
                                       .toEpochMilli()));
    }

    @Override
    public void destroy() throws Exception
    {
        timer.cancel();
    }

    private class TokenRefreshTask extends TimerTask
    {
        @Override
        public void run()
        {
            GoogleIdAndRefreshToken newGoogleIdToken = null;
            WriteLock writeLock = lock.writeLock();
            try {
                newGoogleIdToken = googleTokenRetriever.refreshToken(googleToken.getRefreshToken());
                writeLock.lock();
                googleToken.setIdToken(newGoogleIdToken.getIdToken());

                jweToken = jweTokenRetriever.fetchJweToken(newGoogleIdToken.getIdToken());
            } catch (Throwable e) {
                logger.error("Error while refreshing the id token.", e);
            } finally {
                writeLock.unlock();
                scheduleRefreshTask(newGoogleIdToken == null ? DEFAULT_REFRESH_TASK_FREQUENCY_IN_SECONDS
                                                             : newGoogleIdToken.getExpiresIn());
            }
        }
    }
}
