package ru.gb.smarthome.homekeeper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.structures.Port;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.getFreePort;

@Component
@RequiredArgsConstructor
public class DeviceServerHome implements IDeviceServer
{
    private final HomeService homeService;
    private final PropertyManager propMan;

    private  Thread treadRun;
    private  int    serverSocketPort;


    @PostConstruct private void init ()
    {
        serverSocketPort = propMan.getServerSocketPort();
        Thread t = new Thread (this, "Running Home Server");
        //t.setDaemon (true);
        t.start();
        //if (DEBUG) println ("\nDeviceServerHome.init() завершился.\n");
    }

    @Override public void run ()
    {
        treadRun = Thread.currentThread();
        ExecutorService exeService = Executors.newFixedThreadPool (SMART_PORTS_COUNT/*, new DemonThreadFactory()*/);
        Port port;
        try (ServerSocket servsocket = new ServerSocket (serverSocketPort))
        {
            TimeUnit.SECONDS.sleep(1);
            if (DEBUG) println ("\nСервер ждёт подключения клиентов.");
            while (!treadRun.isInterrupted())
            {
                Socket socket = servsocket.accept ();
                if (DEBUG) println ("\nОт клиента получен запрос на подключение.");

                if (treadRun.isInterrupted())
                    break;
                //Если есть свободный Port, то мы подключим клиента. Иначе, создадим временный хэндлер, только чтобы отправить клиенту отказ.
                if ((port = getFreePort()) != null) {
                    exeService.execute (FactoryHome.createClientHandler (socket, port));
                }
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
