package com.winom.multimedia.exceptions;

import java.io.IOException;

public class ProcessException extends IOException {
    private static final long serialVersionUID = -4943177476157653475L;

    public ProcessException(String detailMessage) {
        super(detailMessage);
    }

    public ProcessException(String detailMessage, Throwable throwable) {
        super("ProcessException: " + detailMessage, throwable);
    }
}
