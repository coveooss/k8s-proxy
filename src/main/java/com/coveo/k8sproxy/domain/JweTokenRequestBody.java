/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.domain;

public class JweTokenRequestBody
{
    private String token;

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public JweTokenRequestBody withToken(String token)
    {
        setToken(token);
        return this;
    }
}
