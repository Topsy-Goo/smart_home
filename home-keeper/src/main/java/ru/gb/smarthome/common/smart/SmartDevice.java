package ru.gb.smarthome.common.smart;

import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

import static ru.gb.smarthome.common.FactoryCommon.*;

public abstract class SmartDevice implements ISmartDevice
{
    protected Socket socket;
    protected ObjectInputStream  ois;
    protected ObjectOutputStream oos;
    protected Thread      threadRun;
    protected Abilities   abilities;
    protected DeviceState state;
    protected AtomicLong  rwCounter = new AtomicLong(0); //счётчик количества чтений из ObjectInputStream и записей в ObjectOutputStream.


    //protected SmartDevice () {}

//------------------------ Реализации интерфейсов ----------------------


    //@Override public Abilities getAbilities () {  return abilities;  }
    //@Override public DeviceState getState () { return state; }

    //@Override public UUID uuid () { return (abilities != null) ? abilities.uuid() : null; }

    /** запрос на отключение устройства, которое, возможно, занято какой-то операцией. */
    @Override public boolean isItSafeToTurnOff () { return false; }

    //@Override public void setPort (Port port) { }
    //@Override public void turnOff () {    }
    //@Override public void sleepSwitch (boolean sleep) {    }
    //@Override public void wakeUp () {    }
    //@Override public boolean canBeMaster () {        return false;    }
    //@Override public boolean canBeSlave () {        return false;    }

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
                Message mCIn = (o instanceof Message) ? (Message) o : null;
//printf("\nПолучили: %s.\n", mCIn);
print ("rM");
                return mCIn;
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
                oos.writeObject (mOut); //TODO: Если клиент упал, то мы продолжаем его опрашивать!!!
                rwCounter.decrementAndGet();
//printf ("\nОтправили: %s\n", mOut);
//print ("wM ");
                return true;
            }
            throw new IOException ("bad ObjectOutputStream passed in.");
        }
        catch (IOException e) { e.printStackTrace();   return false; }
    }
//----------------------------------------------------------------------

//----------------------------------------------------------------------
}
