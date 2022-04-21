package ru.gb.smarthome.fridge;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.gb.smarthome.common.smart.ISmartDevice;

@Configuration
@ComponentScan (basePackages = "ru.gb.smarthome.fridge")
public class FridgeApp {

    public  static final boolean DEBUG = true;
    @SuppressWarnings("all")
    private static       ApplicationContext context;

    public static void main (String[] args)
    {
        if (init ()) {
            context = new AnnotationConfigApplicationContext (FridgeApp.class);
            ISmartDevice device = context.getBean (DeviceClientFridge.class);
            new Thread (device).start();
        }
    }

    @SuppressWarnings("all")
    static boolean init () {
        boolean ok = true;
        return ok;
    }
}
