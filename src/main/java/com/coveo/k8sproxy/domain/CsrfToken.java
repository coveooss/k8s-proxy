/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.domain;

public class CsrfToken
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

    @Override
    public String toString()
    {
        return "CsrfToken [token=" + token + "]";
    }

}
