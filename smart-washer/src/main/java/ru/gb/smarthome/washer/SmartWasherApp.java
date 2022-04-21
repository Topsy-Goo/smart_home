package ru.gb.smarthome.washer;

import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.empty.FactoryEmpty;

//@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.washer")
public class SmartWasherApp {

    public static final boolean         DEBUG = true;
    static              PropertyManager propMan;

    public static void main (String[] args) {

        if (init ()) {
            //SpringApplication.run (SmartWasherApp.class, args);
            ISmartDevice device = new DeviceClientWasher (propMan);
            new Thread (device).start();
        }
    }

    static boolean init () {
        boolean ok = false;
        propMan = FactoryEmpty.getPropertyManager();
        ok = /*propMan.readAllProperties("propertyFile")*/propMan != null;
        return ok;
    }
}
