package ru.gb.smarthome.homekeeper;

import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.structures.Port;

import java.net.Socket;
import java.util.concurrent.SynchronousQueue;

//import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

final public class FactoryHome {

    static ISmartHandler createClientHandler (Socket socket, Port port,
                                              SynchronousQueue<Boolean> helloSynQue, IDeviceServer srv)
    {
        return new ClientHandler (socket, port, helloSynQue, srv);
    }

/** Конструктор используется для создания временного хэндлера, назначение которого только
в том, чтобы сообщить клиенту об отсутствии свободных портов.<p>
Этот конструктор гасит все исключения, чтобы вызывающая ф-ция могла закрыть socket. */
    static void temporaryClientHandler (Socket socket) /*throws IOException*/ {
        new ClientHandler (socket);
    }

/*    static public PropertyManager getPropertyManager () {        return PropertyManagerHome.getInstance();    }*/
/*    static IDeviceServer startDeviceServer () {        return DeviceServerHome.startServer();    }*/

/*    static class DemonThreadFactory implements ThreadFactory
    {
        @Override public Thread newThread (@NotNull Runnable r) {
            Thread t = new Thread (r);
            t.setDaemon (true);
            return t;
        }
    }*/
}
