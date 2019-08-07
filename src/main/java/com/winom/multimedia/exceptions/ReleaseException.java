package com.winom.multimedia.exceptions;

import java.io.IOException;

public class ReleaseException extends IOException {
    private static final long serialVersionUID = 0xc394558814732ddL;

    public ReleaseException(String detailMessage) {
        super(detailMessage);
    }

    public ReleaseException(String detailMessage, Throwable throwable) {
        super("SetupException: " + detailMessage, throwable);
    }
}
