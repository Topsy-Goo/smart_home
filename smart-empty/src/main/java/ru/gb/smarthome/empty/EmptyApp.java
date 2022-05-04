package ru.gb.smarthome.empty;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.empty.client.DeviceClientEmpty;

@Configuration
@ComponentScan (basePackages = "ru.gb.smarthome.empty")
public class EmptyApp {

    public  static final boolean DEBUG = true;
    @SuppressWarnings("all")
    private static ApplicationContext context;

    public static void main (String[] args)
    {
        if (init ()) {
            context = new AnnotationConfigApplicationContext (EmptyApp.class);
            ISmartDevice device = context.getBean (DeviceClientEmpty.class);
            new Thread (device).start();
        }
    }

    @SuppressWarnings("all")
    static boolean init () {
        boolean ok = true;
        return ok;
    }
}
