package ru.gb.smarthome.weatherstation;

import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.empty.FactoryEmpty;

//@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.weatherstation")
public class SmartWeatherStationApp {

    public static final boolean         DEBUG = true;
    static              PropertyManager propMan;

    public static void main (String[] args) {

        if (init ()) {
            //SpringApplication.run (SmartWeatherStationApp.class, args);
            ISmartDevice device = new DeviceClientWeatherStation (propMan);
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
