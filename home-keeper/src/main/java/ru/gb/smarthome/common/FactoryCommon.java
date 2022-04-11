package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_BUSY;

final public class FactoryCommon
{
    public static final int SMART_PORTS_COUNT  = 2;
    public static final int BUSY_SLEEP_SECONDS = 5;
    public static final int    SERVER_PORT_DEFAULT    = 7777;
    public static final String SERVER_ADDRESS_DEFAULT = "localhost";
    public static final boolean ON = true, OFF = false;
    public static final OperationCodes OPCODE_INITIAL = CMD_BUSY; //< состояние, в котором оказывается УУ при запуске его модуля.

/** Проверяет строку на пригодность.
@return true, если ни одна из строк не равна null, не является пустой и не состоит только из пробельных сиволов. */
    public static boolean isStringsValid (String... lines) {
        boolean result = lines != null;
        if (result) {
            for (String s : lines) {
                if (s == null || s.isBlank()) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    public static void println (String s) { System.out.println(s); }
    public static void print (String s) { System.out.print(s); }
    public static void printf (String s, Object... args) { System.out.printf(s, args); }

    public static void errprintln (String s) { System.err.println(s); }
    public static void errprint (String s) { System.err.print(s); }
    public static void errprintf (String s, Object... args) { System.err.printf(s, args); }

/** Считываем Message из подключенного устройства. Блокирующая операция.
@param ois Для общения устройства с сервером создаются две реализации SamrtDevice:
хэндлер (на стороне сервера) и клиент (на стороне устройства).
ois — это экземпляр ObjectInputStream, предоставленный одной из этих реализаций.  */
    public static Message readMessage (ObjectInputStream ois) {
        try {
            if (ois != null) {
                Object o = ois.readObject();
                return (o instanceof Message) ? (Message) o : null;
            }
            else throw new IOException ("bad ObjectInputStream passed in.");
        }
        catch (Exception e) { e.printStackTrace();   return null; }
    }

/** Отправляем сообщение подключенному устройству.
@param oos Для общения устройства с сервером создаются две реализации SamrtDevice:
хэндлер (на стороне сервера) и клиент (на стороне устройства).
oos — это экземпляр ObjectOutputStream, предоставленный одной из этих реализаций.
@param m Отправляемое сообщение.  */
    public static boolean writeMessage (ObjectOutputStream oos, Message m) {
        try {
            if (oos != null) {
                oos.writeObject (m);
                return true;
            }
            throw new IOException ("bad ObjectOutputStream passed in.");
        }
        catch (IOException e) { e.printStackTrace();   return false; }
    }


}
