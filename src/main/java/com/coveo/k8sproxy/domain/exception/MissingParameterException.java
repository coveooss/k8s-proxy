package com.coveo.k8sproxy.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class MissingParameterException extends Exception
{
    public MissingParameterException(String parameter)
    {
        super("Parameter '" + parameter + "' is missing.");
    }
}
