package ru.gb.smarthome.homekeeper;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.common.smart.structures.Port;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ThreadFactory;

//import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

final public class FactoryHome {

    public static final int SMART_PORTS_COUNT  = 2;
    public static final int BUSY_SLEEP_SECONDS = 5;
    public static final int SERVER_PORT_DEFAULT = 7777;

    static public PropertyManager getPropertyManager () {
        return PropertyManagerHome.getInstance();
    }

    static IDeviceServer startDeviceServer () {
        return DeviceServerHome.startServer();
    }

    static ISmartDevice createClientHandler (Socket socket, Port port)/* throws IOException*/ {
        return new ClientHandler (socket, port);
    }

/** Конструктор используется для создания временного хэндлера, назначение которого только
в том, чтобы сообщить клиенту об отсутствии свободных портов.<p>
Этот конструктор гасит все исключения, чтобы вызывающая ф-ция могла закрыть socket. */
    static void temporaryClientHandler (Socket socket) /*throws IOException*/ {
        new ClientHandler (socket);
    }

/*    static class DemonThreadFactory implements ThreadFactory
    {
        @Override public Thread newThread (@NotNull Runnable r) {
            Thread t = new Thread (r);
            t.setDaemon (true);
            return t;
        }
    }*/
}
