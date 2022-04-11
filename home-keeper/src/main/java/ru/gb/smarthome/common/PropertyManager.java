package ru.gb.smarthome.common;

import static ru.gb.smarthome.common.FactoryCommon.*;

abstract public class PropertyManager /*implements IPropertyManager*/ {

    protected int    serverPort    = SERVER_PORT_DEFAULT;
    protected String serverAddress = SERVER_ADDRESS_DEFAULT;


    public boolean setServerSocketPort (int val) {
        boolean ok = val <= 65535 && val >= 0;
        if (ok) serverPort = val;
        return ok;
    }
    public boolean setServerAddress (String val) {
        boolean ok = isStringsValid(val);
        if (ok) serverAddress = val;
        return ok;
    }

    public int getServerSocketPort () {    return serverPort;    }
    public String getServerAddress () {    return serverAddress;    }


}
