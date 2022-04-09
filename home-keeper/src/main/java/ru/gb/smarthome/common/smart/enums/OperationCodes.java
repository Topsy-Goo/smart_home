package ru.gb.smarthome.common.smart.enums;

import java.io.Serializable;
import java.util.HashMap;

public enum OperationCodes implements Serializable {
    CMD_CONNECTED,
    CMD_BUSY,
    CMD_UUID,
    CMD_EXIT
    ;

    private static final HashMap<String, OperationCodes> map;

    static {
        OperationCodes[] arr = values();
        map = new HashMap<>(arr.length +1, 1.0F); //< +1, т.к. loadFactor указывает, сколько % должно быть заполнено, чтобы set увеличился. С +1 у нас не будет 100%-го заполнения.
        for (OperationCodes oc : arr) {
            map.put(oc.name(), oc);
        }
    }

    public static OperationCodes byName (String name) {
        return map.get (name.trim());
    }
}
