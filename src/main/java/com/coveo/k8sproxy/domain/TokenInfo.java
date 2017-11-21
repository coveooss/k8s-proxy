/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.domain;

public class TokenInfo
{
    private String idToken;
    private String refreshToken;

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

    public TokenInfo withIdToken(String idToken)
    {
        setIdToken(idToken);
        return this;
    }

    public TokenInfo withRefreshToken(String refreshToken)
    {
        setRefreshToken(refreshToken);
        return this;
    }
}
