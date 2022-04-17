package ru.gb.smarthome.homekeeper;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.structures.Port;

import java.util.ArrayList;
import java.util.List;

import static ru.gb.smarthome.common.FactoryCommon.SMART_PORTS_COUNT;

@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.homekeeper")
public class HomeKeeperApp
{
    public static final boolean DEBUG = true;
    public static final List<Port> ports = new ArrayList<>(SMART_PORTS_COUNT);

    public static void main (String[] args)
    {
        if (init ()) {
            SpringApplication.run (HomeKeeperApp.class, args);
            //FactoryHome.startDeviceServer ();
        }
    }

    private static boolean init () {
        boolean ok = false;

        for (int i=0;  i < SMART_PORTS_COUNT;  i++) {
            ports.add (new Port());
        }
        if (initFlyway()) {
            //if (propMan.readAllProperties ("propertyFile"))
                ok = true;
        }
        return ok;
    }

/** Находим свободный Port в ports. */
    public static Port getFreePort () {
        for (Port p : ports)
            if (p.isFree())
                return p;
        return null;
    }

    private static boolean initFlyway () {
/*        Flyway flyway = Flyway.configure()
                              .dataSource ("jdbc:h2://localhost:3306./target/foobar", "root", null)
                              .load();
        flyway.migrate();*/
        return true;
    }

    //public static Port f () {}
}
