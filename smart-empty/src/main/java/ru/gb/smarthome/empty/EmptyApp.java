package ru.gb.smarthome.empty;

import org.springframework.context.annotation.Configuration;
import ru.gb.smarthome.common.PropertyManager;


//@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.empty")
//@ComponentScan (basePackages = "ru.gb.smarthome.empty")
//@Configuration
public class EmptyApp {

    //@Value("${application.homekeeper.port}")  <<< J9,lesson7
    static final boolean   DEBUG = true;
    static PropertyManager propMan;

    public static void main (String[] args)
    {
        //SpringApplication.run (EmptyApp.class, args);
        if (init ())
            new Thread (new DeviceClientEmpty (propMan), "Running Empty Device").start();
    }

    static boolean init () {
        boolean ok = false;
        propMan = FactoryEmpty.getPropertyManager();
        ok = propMan.readAllProperties("propertyFile");
        return ok;
    }

}
