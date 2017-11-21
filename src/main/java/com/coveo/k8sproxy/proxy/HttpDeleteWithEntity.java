/**
 * Copyright (c) 2011 - 2017, Coveo Solutions Inc.
 */
package com.coveo.k8sproxy.proxy;

import java.net.URI;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpDeleteWithEntity extends HttpEntityEnclosingRequestBase
{
    public HttpDeleteWithEntity(String uri)
    {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod()
    {
        return HttpDelete.METHOD_NAME;
    }
}
