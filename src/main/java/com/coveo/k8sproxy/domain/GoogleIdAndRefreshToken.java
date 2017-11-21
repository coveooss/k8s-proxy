/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GoogleIdAndRefreshToken
{
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("expires_in")
    private long expiresIn;
    @JsonProperty("id_token")
    private String idToken;

    public long getExpiresIn()
    {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn)
    {
        this.expiresIn = expiresIn;
    }

    public String getIdToken()
    {
        return idToken;
    }

    public void setIdToken(String idToken)
    {
        this.idToken = idToken;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }

    public GoogleIdAndRefreshToken withRefreshToken(String refreshToken)
    {
        setRefreshToken(refreshToken);
        return this;
    }

    public GoogleIdAndRefreshToken withIdToken(String idToken)
    {
        setIdToken(idToken);
        return this;
    }

    public GoogleIdAndRefreshToken withExpiresIn(long expiresIn)
    {
        setExpiresIn(expiresIn);
        return this;
    }
}
