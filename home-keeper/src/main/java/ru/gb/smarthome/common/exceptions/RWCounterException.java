package ru.gb.smarthome.common.exceptions;

public class RWCounterException extends Exception {

    public RWCounterException () {
        super();
    }

    public RWCounterException (String s) {
        super(s);
    }

    public RWCounterException (Throwable e) {
        super(e);
    }

    public RWCounterException (String s, Throwable e) {
        super(s,e);
    }
}
