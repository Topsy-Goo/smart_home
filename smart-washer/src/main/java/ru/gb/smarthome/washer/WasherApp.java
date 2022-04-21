package ru.gb.smarthome.washer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.gb.smarthome.common.smart.ISmartDevice;

@Configuration
@ComponentScan (basePackages = "ru.gb.smarthome.washer")
public class WasherApp {

    public static final boolean            DEBUG = true;
    @SuppressWarnings("all")
    private static      ApplicationContext context;

    public static void main (String[] args) {

        if (init ()) {
            context = new AnnotationConfigApplicationContext (WasherApp.class);
            ISmartDevice device = context.getBean (DeviceClientWasher.class);
            new Thread (device).start();
        }
    }

    @SuppressWarnings("all")
    static boolean init () {
        boolean ok = true;
        return ok;
    }
}
