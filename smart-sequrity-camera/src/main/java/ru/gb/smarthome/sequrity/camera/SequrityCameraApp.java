package ru.gb.smarthome.sequrity.camera;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.sequrity.camera.client.DevClientSequrCamera;

@Configuration
@ComponentScan (basePackages = "ru.gb.smarthome.sequrity.camera")
public class SequrityCameraApp {

    public static final boolean            DEBUG = true;
    @SuppressWarnings("all")
    private static      ApplicationContext context;

	public static void main(String[] args) {
        if (init ()) {
            context = new AnnotationConfigApplicationContext (SequrityCameraApp.class);
            ISmartDevice device = context.getBean (DevClientSequrCamera.class);
            new Thread (device).start();
        }
	}

    @SuppressWarnings("all")
    static boolean init () {
        boolean ok = true;
        return ok;
    }
}
