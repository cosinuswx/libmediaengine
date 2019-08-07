package com.winom.multimedia.exceptions;

import java.io.IOException;

public class SetupException extends IOException {
    private static final long serialVersionUID = 992254983224751875L;

    public SetupException(String detailMessage) {
        super(detailMessage);
    }

    public SetupException(String detailMessage, Throwable throwable) {
        super("SetupException: " + detailMessage, throwable);
    }
}
