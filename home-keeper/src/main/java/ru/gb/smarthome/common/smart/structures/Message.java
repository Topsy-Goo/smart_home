package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;

import java.io.Serializable;
import java.util.UUID;

import static java.lang.String.format;

/** Структура для обмена данными между УД и подключеннми к нему УУ. */
public class Message implements Serializable {

    @Getter private OperationCodes opCode;
    @Getter private UUID deviceUUID;
    @Getter private Object data;

    public Message () {} //< условие сериализации
    public Message (@NotNull OperationCodes oc, UUID uu, Object o) {
        opCode = oc;
        deviceUUID = uu;
        data = o;
    }
    public Message copy () {
        return new Message(opCode, deviceUUID, data);
    }

/*  public OperationCodes getOpCode () { return opCode; }
    public UUID getDeviceUUID () { return deviceUUID; }
    public Object getData () { return data; }*/
    public Message setDeviceUUID (UUID val) { deviceUUID = val;   return this; }
    public Message setOpCode (OperationCodes val) { opCode = val;   return this; }
    public Message setData (Object val) { data = val;   return this; }

    @Override public String toString () {
        return format ("Message[%s | uuid:%s | data:%s]", opCode.name(), deviceUUID, data);
    }
}
//Если при сериализации А таже сериализуются подтипы несериализуемых классов, то такие подтипы нужно снабжать умолчальными конструкторами, доступными из А.
