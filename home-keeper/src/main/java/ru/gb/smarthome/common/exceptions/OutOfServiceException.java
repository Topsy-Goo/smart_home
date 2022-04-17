package ru.gb.smarthome.common.exceptions;

public class OutOfServiceException extends Exception {

    public OutOfServiceException () {
        super();
    }

    public OutOfServiceException (String s) {
        super(s);
    }

    public OutOfServiceException (Throwable e) {
        super(e);
    }

    public OutOfServiceException (String s, Throwable e) {
        super(s,e);
    }

}
