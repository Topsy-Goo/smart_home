package ru.gb.smarthome.common;

import static ru.gb.smarthome.common.FactoryCommon.isStringValid;
import static ru.gb.smarthome.homekeeper.FactoryHome.SERVER_PORT_DEFAULT;

abstract public class PropertyManager /*implements IPropertyManager*/ {

    private int    serverSocketPort;
    private String serverAddress;

//Мнимальная реализация. Может подойти простым устройствам.
    //TODO:Пусть свойства считываются в Map<String, String>.
    // Наверное, можно этот мэп создавать заранее и заполнять умолчальными значениями.
    public boolean readAllProperties (String fileName) {
        boolean ok = true;
    //TODO:имитация чтения из файла настроек (позже файл будет):
        serverSocketPort = SERVER_PORT_DEFAULT;
        serverAddress = "localhost";
        return ok;
    }

    public boolean setServerSocketPort (int val) {
        boolean ok = val <= 65535 && val >= 0;
        if (ok) serverSocketPort = val;
        return ok;
    }
    public boolean setServerAddress (String val) {
        boolean ok = isStringValid (val);
        if (ok) serverAddress = val;
        return ok;
    }

    public int getServerSocketPort () {    return serverSocketPort;    }
    public String getServerAddress () {    return serverAddress;    }


}
