package ru.gb.smarthome.homekeeper;

import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Port;

import java.io.*;
import java.net.Socket;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TransferQueue;

import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;
import static ru.gb.smarthome.common.FactoryCommon.*;

public class ClientHandler extends SmartDevice
{
    private final Message messagingMonitor = new Message();
    private       Port    port;
    private       Socket socket;

    public ClientHandler (Socket s, Port p)
    {
        if (DEBUG && (s == null || p == null))
            throw new IllegalArgumentException();
        port = p;
        socket = s;
        p.occupy (socket, this);
        //TODO:здесь мы должны поставить в очередь запросы CMD_UUID и CMD_TYPE
    }

/** Конструктор используется для создания временного хэндлера, назначение которого только
в том, чтобы сообщить клиенту об отсутствии свободных портов.<p>
Этот конструктор гасит все исключения, чтобы вызывающая ф-ция могла закрыть socket.  */
    public ClientHandler (Socket s) {
        socket = s;
        try {
            oos = new ObjectOutputStream (socket.getOutputStream());
            ois = new ObjectInputStream (socket.getInputStream());

            OperationCodes opCode = CMD_NOPORTS;
            String codeName = opCode.name();
            writeMessage (oos, new Message().setOpCode (opCode));

            printf ("\nотправлено соощение: %s.", codeName);
            if (DEBUG) println ("\nКлиенту отказано в подключении, — нет свободных портов.");
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void run ()
    {
        threadRun = Thread.currentThread();
        String code = "OK";
        try {
            oos = new ObjectOutputStream (socket.getOutputStream());
            ois = new ObjectInputStream (socket.getInputStream());
            //Совершенно неожиданно оказалось, что одинаковые операции — две ObjectOutputStream или две ObjectInputStream — блокируют друг друга, кода вызываются на обоих концах канала. Поэтому, если на одном конце канала вызывается, например, new ObjectInputStream(…), то на другом нужно обязательно вызвать new ObjectOutputStream(…), чтобы не случилась взаимная блокировка.
            printf ("\nСоединение с клиентом установлено: "+
                    "\nsocket : %s (opend: %b)"+
                    "\nois : %s"+
                    "\noos : %s\n", socket, !socket.isClosed(), ois, oos);
            mainLoop();
        }
        catch (Exception e) {
            code = e.getMessage();
            if (DEBUG) e.printStackTrace();
        }
        finally {
            disconnect();
            Thread.yield(); //< возможно, это позволит вовремя вывести сообщение об ошибке.
            if (DEBUG) printf ("\nПоток %s завершился. Код завершения: %s.\n", threadRun, code);
        }
    }

/** Очистка в конце работы метода run(). */
    private void disconnect()
    {
        try {
            if (socket != null) socket.close();
        }
        catch (Exception e) { e.printStackTrace(); }
        finally {
            if (port != null) port.freePort();
            port = null;
            socket = null;
        }
    }

/** Основной цикл клиента. */
    private void mainLoop ()
    {
        OperationCodes opCode = CMD_CONNECTED;
        Message m = new Message().setOpCode (opCode);
        writeMessage (oos, m);
        try {
            while (!threadRun.isInterrupted())
            {
                if ((m = readMessage (ois)) != null) //< блокирующая операция
                //String  cmd = dis.readUTF().trim();
                //OperationCodes opCode = OperationCodes.byName(cmd);
                //if (opCode != null)
                switch (opCode = m.getOpCode())
                {
                    case CMD_EXIT:
                        cleanup();
                        threadRun.interrupt();
                        break;

                    default: if (DEBUG)
                        throw new UnsupportedOperationException ("Неизвестный код операции: "
                                        + opCode.name());
                }
                else if (DEBUG)
                    throw new RuntimeException ("Неправильный тип считанного объекта.");
            }//while
        }
        //catch (IOException e) { e.printStackTrace(); }
        catch (Exception e) { e.printStackTrace(); }
        finally {
            cleanup();
            println ("\nВыход из mainLoop().");
        }
    }

/** Очистка в конце работы метода mainLoop(). */
    private void cleanup () {}

//---------------------- Реализации методов: -------------------------------
//TODO:неправильная обработка запроса. Будет переделана позже.
    @Override public UUID uuid () {
        if (uuid == null)
            synchronized (messagingMonitor) {
                if (writeMessage(oos, messagingMonitor
                                       .setOpCode (CMD_UUID)
                                       .setData (null)
                                       .setDeviceUUID (null)))
                {
                    Message m = readMessage(ois); //< блокирующая операция
                    if (m != null)
                        uuid = m.getDeviceUUID();
                }
            }
        return uuid;
    }

    void f () {
        ;
        //* Поток УД помещает сюда команды к нашему УУ.
        //* Команды выполняются в порядке добавления (FIFO).
        //* ?Если команда не может быть выполнена из-за того, что УУ занят более важным делом,
        //  то ищем в очереди команду с подходящим приоритетом?

        //TransferQueue

        LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
        //* FIFO

        PriorityBlockingQueue<Message> pbq;
        //* предоставляет блокирующие операции извлечения
        //* не допускает пустых элементов
        //* iterator и spliterator не гарантируют обход элементов в каком-либо конкретном порядке. Если нужен упорядоченный обход, рассмотрите Arrays.sort(pq.toArray()).
        //* drainTo() — для переноса элементов в другую коллекцию в порядке приоритета.
        //* никаких гарантий относительно упорядочения элементов с равным приоритетом.
    }
}
