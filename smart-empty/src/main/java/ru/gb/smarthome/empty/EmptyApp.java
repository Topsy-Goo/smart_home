package ru.gb.smarthome.empty;

import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.ISmartDevice;

//import static ru.gb.smarthome.common.smart.SmartDevice.runConsole;

//@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.empty")
public class EmptyApp {

    static final boolean   DEBUG = true;
    static PropertyManager propMan;

    public static void main (String[] args)
    {
        if (init ()) {
            //SpringApplication.run (EmptyApp.class, args);
            ISmartDevice device = new DeviceClientEmpty (propMan);
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
