package ru.gb.smarthome.homekeeper;

import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.structures.Port;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.println;
import static ru.gb.smarthome.homekeeper.FactoryHome.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.getFreePort;

public class DeviceServerHome implements IDeviceServer
{
    private static       DeviceServerHome instance;
    private static final Object           monitor = new Object();

    private final PropertyManager propMan;
    private       Thread treadRun;
    private final int    serverSocketPort;


    private DeviceServerHome () {
        propMan = HomeKeeperApp.getPropManager();
        if (propMan == null)
            throw new RuntimeException();
        serverSocketPort = propMan.getServerSocketPort();
    }

    public static IDeviceServer startServer() {
        if (instance == null)
            synchronized (monitor) {
                if (instance == null) {
                    instance = new DeviceServerHome();
                    Thread t = new Thread (instance, "Running Home Server");
                    //t.setDaemon (true);
                    t.start();
                    ;
                }
            }
        return instance;
    }

    @Override public void run ()
    {
        treadRun = Thread.currentThread();
        ExecutorService exeService = Executors.newFixedThreadPool(
                                                SMART_PORTS_COUNT  //< +1 для информирования лишних
                                                /*, new DemonThreadFactory()*/);
        Port port;
        try (ServerSocket servsocket = new ServerSocket (serverSocketPort))
        {
            if (DEBUG) println ("\nСервер ждёт подключения клиентов.");
            while (!treadRun.isInterrupted())
            {
                Socket socket = servsocket.accept ();
                if (DEBUG) println ("\nОт клиента получен запрос на подключение.");

                if (treadRun.isInterrupted())
                    break;
            //Если есть свободный Port, то мы подключим клиента. Иначе, создадим временный
            // хэндлер, только чтобы отправить клиенту отказ.
                if ((port = getFreePort()) != null)
                    exeService.execute (FactoryHome.createClientHandler (socket, port));
                else {
                    FactoryHome.temporaryClientHandler (socket);
                    socket.close();
                    TimeUnit.SECONDS.sleep (BUSY_SLEEP_SECONDS);
                }
            }//while
        }
        catch (IOException | InterruptedException e) { e.printStackTrace(); }
        catch (Exception e) { e.printStackTrace(); }
        finally {
            println ("\nПодключение клиентов прекращено.\n");
            /*...*/
        }
    }


}
