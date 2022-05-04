package ru.gb.smarthome.common.smart;

import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Task;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SmartDevice implements ISmartDevice
{
    protected       Socket socket;
    protected       ObjectInputStream  ois;
    protected       ObjectOutputStream oos;
    protected       Thread      threadRun;
    protected       Abilities   abilities;
    protected       DeviceState state;
    protected final AtomicLong  rwCounter = new AtomicLong(0); //счётчик количества чтений из ObjectInputStream и записей в ObjectOutputStream.

//----------------------------------------------------------------------

//----------------------------------------------------------------------

/** Считываем Message из подключенного устройства. Блокирующая операция.
@param ois Для общения устройства с сервером создаются две реализации SamrtDevice:
хэндлер (на стороне сервера) и клиент (на стороне устройства).
ois — это экземпляр ObjectInputStream, предоставленный одной из этих реализаций.  */
    public Message readMessage (ObjectInputStream ois) {
        try {
            if (ois != null) {
                Object o = ois.readObject();
                rwCounter.incrementAndGet();
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
@param mOut Отправляемое сообщение.  */
    public boolean writeMessage (ObjectOutputStream oos, Message mOut)
    {
        try {
            if (oos != null) {
                oos.writeObject (mOut);
                rwCounter.decrementAndGet();
                return true;
            }
            throw new IOException ("bad ObjectOutputStream passed in.");
        }
        catch (IOException e) { e.printStackTrace();   return false; }
    }
//----------------------------------------------------------------------

/** Поиск задачи в Abilities.tasks по указаному параметру data.
 @param data может быть типов Task или String. Во втором случае он расценивается как Task.name.  */
    protected Task findTask (Object data)
    {
        if (abilities == null)
            return null;
        return abilities.getTasks().stream().filter ((tsk)->(tsk.equals (data)))
                                   .findFirst().orElse (null);
    }

//----------------------------------------------------------------------
}
