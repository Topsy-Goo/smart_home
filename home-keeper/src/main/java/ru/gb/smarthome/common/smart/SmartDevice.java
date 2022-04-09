package ru.gb.smarthome.common.smart;

import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Port;

import java.io.*;
import java.util.UUID;

public abstract class SmartDevice implements ISmartDevice
{
    //protected DataInputStream   dis;
    //protected DataOutputStream  dos;
    protected ObjectInputStream ois;
    protected ObjectOutputStream oos;
    protected       Thread threadRun;
    protected       UUID uuid;

    //protected SmartDevice () {    }


    @Override
    public DeviceTypes deviceClass () {
        return null;
    }

//  @Override public UUID uuid () {  return null;  }

    @Override
    public DeviceStatus status () {
        return null;
    }

/*    @Override     < не нужно уже
    public void setPort (Port port) {

    }*/

    @Override
    public void setName (String name) {

    }

    @Override
    public void turnOn () {

    }

    @Override
    public void turnOff () {

    }

    @Override
    public void sleep () {

    }

    @Override
    public void wakeUp () {

    }

    /** запрос на отключение устройства, которое, возможно, занято какой-то операцией. */
    @Override
    public boolean isItSafeTurnOff () {
        return false;
    }

    @Override
    public boolean canBeMaster () {
        return false;
    }

    @Override
    public boolean canBeSlave () {
        return false;
    }


}
