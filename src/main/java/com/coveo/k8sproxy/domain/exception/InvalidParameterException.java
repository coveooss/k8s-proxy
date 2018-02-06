package com.coveo.k8sproxy.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidParameterException extends Exception
{
    private static final long serialVersionUID = 1L;

    public InvalidParameterException(String parameter, String message)
    {
        super("Parameter '" + parameter + "' " + message);
    }
}
