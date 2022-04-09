package ru.gb.smarthome.empty;

import ru.gb.smarthome.common.PropertyManager;

//import static ru.gb.smarthome.empty.EmptyApp.DEBUG;

public class PropertyManagerEmpty extends PropertyManager
{
    private static       PropertyManager instance;
    private static final Object          monitor = new Object();


    private PropertyManagerEmpty () {}

    public static PropertyManager getInstance() {
        if (instance == null)
            synchronized (monitor) {
                if (instance == null)
                    instance = new PropertyManagerEmpty();
            }
        return instance;
    }

    //@Override public boolean readFile (String fileName) {    return super.readFile (fileName);    }

    //@Override public int getServerPort () {    return super.getServerPort();    }

    //@Override public String getServerAddress () {    return super.getServerAddress();    }

}
