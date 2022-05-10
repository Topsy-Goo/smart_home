package ru.gb.smarthome.homekeeper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.structures.Port;
import ru.gb.smarthome.homekeeper.services.HomeService;

import javax.annotation.PostConstruct;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.getFreePort;

@Component
@RequiredArgsConstructor
public class DeviceServerHome implements IDeviceServer
{
    private final HomeService     homeService;
    private final PropertyManager propMan;
    private       Thread treadRun;
    private       int serverSocketPort;


    @PostConstruct private void init ()
    {
        serverSocketPort = propMan.getServerSocketPort();
        Thread t = new Thread (this, "Running Home Server");
        //t.setDaemon (true);
        t.start();
    }

    @Override public void run ()
    {
        treadRun = Thread.currentThread();
        ExecutorService exeService = Executors.newFixedThreadPool (SMART_PORTS_COUNT);
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
                //Если есть свободный Port, то мы подключим клиента. Иначе, создадим
                // временный хэндлер, только чтобы отправить клиенту отказ.
                if ((port = getFreePort()) != null)
                {
                    SynchronousQueue<Boolean> helloSynQue = new SynchronousQueue<>();
                    ISmartHandler device = createClientHandler (
                                                socket, port, helloSynQue, this,
                                                homeService::slaveCallback, homeService::addNews);
                    exeService.execute (device);
                    if (helloSynQue.take())
                        homeService.smartDeviceDetected (device);
                }
                else {
                    temporaryClientHandler (socket);
                    socket.close();
                    TimeUnit.SECONDS.sleep (BUSY_SLEEP_SECONDS);
                }
            }//while
        }
        //catch (IOException | InterruptedException e) { e.printStackTrace(); }
        catch (Exception e) { e.printStackTrace(); }
        finally {    println ("\nПодключение клиентов прекращено.\n");    }
    }

    @Override public void goodBy (ISmartHandler device) {
        homeService.smartDeviceIsOff (device);
    }

    private static ISmartHandler createClientHandler (
                                    Socket socket, Port port,
                                    SynchronousQueue<Boolean> helloSynQue,
                                    IDeviceServer srv, ISignalCallback slaveCallback
                                    , IAddNewsCallback addNewsCallbac)
    {
        return new ClientHandler (socket, port, helloSynQue, srv, slaveCallback, addNewsCallbac);
    }

/** Конструктор используется для создания временного хэндлера, назначение которого только
в том, чтобы сообщить клиенту об отсутствии свободных портов.<p>
Этот конструктор гасит все исключения, чтобы вызывающая ф-ция могла закрыть socket. */
    static void temporaryClientHandler (Socket socket) {
        new ClientHandler (socket);
    }
}
