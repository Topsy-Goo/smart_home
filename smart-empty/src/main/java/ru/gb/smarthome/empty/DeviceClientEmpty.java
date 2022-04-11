package ru.gb.smarthome.empty;

import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.exceptions.OutOfService;
import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SMART;
import static ru.gb.smarthome.empty.EmptyApp.DEBUG;

public class DeviceClientEmpty extends SmartDevice
{
    private final PropertyManager propMan;
    private       OperationCodes mode = OPCODE_INITIAL;   //< временно будет изображать состояние УУ

    public DeviceClientEmpty (PropertyManager pm) {
        propMan = pm;
        deviceType = SMART;
        uuid = UUID.randomUUID();
    }

    @Override public void run ()
    {
        threadRun = Thread.currentThread();
        String code = "OK";
        try (Socket socket = connect())
        {
            ois = new ObjectInputStream (socket.getInputStream());
            oos = new ObjectOutputStream (socket.getOutputStream());
            //Совершенно неожиданно оказалось, что одинаковые операции — две ObjectOutputStream или две ObjectInputStream — блокируют друг друга, кода вызываются на обоих концах канала. Поэтому, если на одном конце канала вызывается, например, new ObjectInputStream(…), то на другом нужно обязательно вызвать new ObjectOutputStream(…), чтобы не случилась взаимная блокировка.
            printf ("\nСоединение с сервером установлено: "+
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

    private Socket connect () throws IOException
    {
        String address = propMan.getServerAddress();
        int port       = propMan.getServerSocketPort();

        Socket socket = new Socket (address, port);
        //(Стримы нельзя здесь получать, — если они взбрыкнут, то метод не вернёт socket.)
        if (DEBUG) printf ("\nСоединение с сервером установлено (адрес:%s, порт:%d).\n", address, port);
        return socket;
    }

/** Очистка в конце работы метода run(). */
    private void disconnect() {}

/** Основной цикл клиента. */
    private void mainLoop () //throws InterruptedException
    {
        Message m;
        OperationCodes opCode;
        try
        {   while (!threadRun.isInterrupted())
            {
                if ((m = readMessage (ois)) != null) //< блокирующая операция
                //String cmd = dis.readUTF().trim();
                //opCode = OperationCodes.byName(cmd);
                //if (opCode != null)
                switch (opCode = m.getOpCode())
                {
                    case CMD_CONNECTED: println ("\nПодключен.");
                        break;

                    case CMD_NOPORTS:
                        throw new OutOfService ("!!! Отказано в подключении, — нет свободных портов. !!!");

                    default: if (DEBUG)
                        throw new UnsupportedOperationException ("Неизвестный код операции: "
                                        + opCode.name());
                }
                else if (DEBUG)
                    throw new RuntimeException ("Неправильный тип считанного объекта.");
            }//while
        }
        catch (OutOfService e) {  println ("\n"+ e.getMessage());  }
        catch (Exception e)    {  e.printStackTrace();  }
        finally {
            cleanup();
            println ("\nВыход из mainLoop().");
        }
    }

/** Очистка в конце работы метода mainLoop(). */
    private void cleanup () {}

//---------------------------------------------------------------------------

//TODO: тоже неправильная реализация. Позже будет переделана.
    @Override public UUID uuid () { return uuid; }
}
