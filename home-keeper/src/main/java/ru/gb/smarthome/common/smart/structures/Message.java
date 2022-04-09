package ru.gb.smarthome.common.smart.structures;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;

import java.io.Serializable;
import java.util.UUID;

import static java.lang.String.format;

/** Структура для обмена данными между УД и подключеннми к нему УУ. */
public class Message implements Serializable {

    private OperationCodes opCode;
    private UUID deviceUUID;
    private Object data;

    public Message () {} //< условие сериализации

    public Message (@NotNull OperationCodes oc, UUID uu, Object o) {
        opCode = oc;
        deviceUUID = uu;
        data = o;
    }

    //public Message set (@NotNull OperationCodes oc, ISmartDevice device, Object o) {
    //    return this;
    //}

    public UUID getDeviceUUID () { return deviceUUID/*new UUID (mostUUIDbits, leastUUIDbits)*/; }
    public OperationCodes getOpCode () { return opCode; }
    public Object getData () { return data; }

    public Message setDeviceUUID (UUID val) { deviceUUID = val;   return this; }
    public Message setOpCode (OperationCodes val) { opCode = val;   return this; }
    public Message setData (Object val) { data = val;   return this; }

    @Override public String toString () {
        return format ("Message[uuid: %s, data: %s]", deviceUUID/*getUUID()*/, data);
    }
}
