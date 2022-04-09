package ru.gb.smarthome.homekeeper;

import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Port;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;
import static ru.gb.smarthome.common.FactoryCommon.*;

public class ClientHandler extends SmartDevice
{
    private final Message deviceRequestMonitor = new Message();
    private       Port port;
    private       Socket socket;

    public ClientHandler (Socket s, Port p) {
        port = p;
        socket = s;
        p.occupy (socket, this);
    }

/** Конструктор используется для создания временного хэндлера, назначение которого только
в том, чтобы сообщить клиенту об отсутствии свободных портов.<p>
Этот конструктор гасит все исключения, чтобы вызывающая ф-ция могла закрыть socket.  */
    public ClientHandler (Socket s) {
        socket = s;
        try {
            oos = new ObjectOutputStream (socket.getOutputStream());
            ois = new ObjectInputStream (socket.getInputStream());

            String codeName = CMD_BUSY.name();
            writeMessage (oos, new Message().setOpCode (CMD_BUSY));

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
            //Совершенно неожиданно оказалось, что одинаковые операции — две ObjectOutputStream или две ObjectInputStream — блокируют друг друга, кода вызываются на обоих концах канала. Поэтому, если на одном конце канала вызывается, например, new ObjectInputStream(…), то на другом нужно обязательно вызвать new ObjectOutputStream(…), чтобы не случилась взаимная блокировка.

            oos = new ObjectOutputStream (socket.getOutputStream());
            ois = new ObjectInputStream (socket.getInputStream());
            printf ("\nСоединение с клиентом установлено: "+
                    "\nsocket : %s (opend: %b)"+
                    "\nois : %s"+
                    "\noos : %s\n", socket, !socket.isClosed(), ois, oos);
            mainLoop();
        }
        catch (IOException e) {
            if (DEBUG) e.printStackTrace();
            code = "IOError";
        }
        catch (Exception e) {
            if (DEBUG) e.printStackTrace();
            code = "Error";
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

//-------------------------------------------------------------------------------

    @Override public UUID uuid () {
        UUID result = null;
        synchronized (deviceRequestMonitor) {
            if (writeMessage(oos, deviceRequestMonitor
                               .setOpCode (CMD_UUID)
                               .setData (null)
                               .setDeviceUUID (null)))
            {
                Message m = readMessage(ois); //< блокирующая операция
                if (m != null)
                    result = m.getDeviceUUID();
            }
        }
        return result;
    }
}
