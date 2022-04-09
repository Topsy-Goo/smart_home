package ru.gb.smarthome.homekeeper;

import ru.gb.smarthome.common.PropertyManager;

//import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

public class PropertyManagerHome extends PropertyManager
{
    private static       PropertyManager instance;
    private static final Object          monitor = new Object();


    private PropertyManagerHome () {}

    public static PropertyManager getInstance() {
        if (instance == null)
            synchronized (monitor) {
                if (instance == null)
                    instance = new PropertyManagerHome();
            }
        return instance;
    }

    //@Override public boolean readFile (String fileName) {    return super.readFile (fileName);    }

    //@Override public int getServerSocketPort () {    return super.getServerSocketPort();    }

    //@Override public String getServerAddress () {    return super.getServerAddress();    }

}
