package ru.gb.smarthome.homekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.structures.Port;

import java.util.ArrayList;
import java.util.List;

import static ru.gb.smarthome.homekeeper.FactoryHome.SMART_PORTS_COUNT;

@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.homekeeper")
public class HomeKeeperApp
{
    static final boolean         DEBUG = true;
    static       PropertyManager propMan;
    static final List<Port>      ports = new ArrayList<>(SMART_PORTS_COUNT);

    public static void main (String[] args)
    {
        if (init ()) {
            SpringApplication.run (HomeKeeperApp.class, args);
            FactoryHome.startDeviceServer ();
        }
    }

    public static PropertyManager getPropManager () { return propMan; }

    private static boolean init () {
        boolean ok = false;
        for (int i=0;  i < SMART_PORTS_COUNT;  i++) {
            ports.add (new Port());
        }
        propMan = FactoryHome.getPropertyManager();
        ok = propMan.readAllProperties("propertyFile");
        return ok;
    }

/** Находим свободный Port в ports. */
    public static Port getFreePort () {
        for (Port p : ports)
            if (p.isFree())
                return p;
        return null;
    }

    //public static Port f () {}
}
