package ru.gb.smarthome.weatherstation;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.gb.smarthome.common.smart.ISmartDevice;

@Configuration
@ComponentScan (basePackages = "ru.gb.smarthome.weatherstation")
public class WeatherStationApp {

    public static final boolean DEBUG = true;
    @SuppressWarnings("all")
    private static      ApplicationContext context;

    public static void main (String[] args) {

        if (init ()) {
            context = new AnnotationConfigApplicationContext (WeatherStationApp.class);
            ISmartDevice device = context.getBean (DeviceClientWeatherStation.class);
            new Thread (device).start();
        }
    }

    @SuppressWarnings("all")
    static boolean init () {
        boolean ok = true;
        return ok;
    }
}
