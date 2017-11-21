/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.domain;

public class JweToken
{
    private String jweToken;
    private String encodedJweToken;

    public String getJweToken()
    {
        return jweToken;
    }

    public void setJweToken(String jweToken)
    {
        this.jweToken = jweToken;
    }

    public String getEncodedJweToken()
    {
        return encodedJweToken;
    }

    public void setEncodedJweToken(String encodedJweToken)
    {
        this.encodedJweToken = encodedJweToken;
    }

    @Override
    public String toString()
    {
        return "JweToken [jweToken=" + jweToken + "]";
    }

}
