package ru.gb.smarthome.common.exceptions;

public class OutOfService extends Exception {

    public OutOfService () {
        super();
    }

    public OutOfService (String s) {
        super(s);
    }

    public OutOfService (Throwable e) {
        super(e);
    }

    public OutOfService (String s, Throwable e) {
        super(s,e);
    }

}
