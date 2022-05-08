package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.enums.OperationCodes;

import java.io.Serializable;

import static java.lang.String.format;

/** Структура для обмена данными между УД и подключеннми к нему УУ. */
public class Message implements Serializable, Comparable<Message> {

    @Getter private OperationCodes opCode;
    @Getter private Object data;

    public Message () {} //< условие сериализации
    public Message (@NotNull OperationCodes oc, Object o) {
        opCode = oc;
        data = o;
    }
    public Message copy () {
        return new Message (opCode, /*deviceUUID,*/ data);
    }

/*  public OperationCodes getOpCode () { return opCode; }
    public Object getData () { return data; }*/
    public Message setOpCode (OperationCodes val) { opCode = val;   return this; }
    public Message setData (Object val)           { data = val;   return this; }

    @Override public String toString () {
        return format ("Message[%s | data:%s]", opCode.name(), data);
    }

    @Override public int compareTo (@NotNull Message other)
    {
        if (opCode != null && other.opCode != null)
            return opCode.compareTo(other.opCode);
        throw new RuntimeException("Message.opCode == null !!!");
    }
}
//Если при сериализации А таже сериализуются подтипы несериализуемых классов, то такие
// подтипы нужно снабжать умолчальными конструкторами, доступными из А.
