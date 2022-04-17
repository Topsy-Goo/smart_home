package ru.gb.smarthome.common.smart;


import lombok.Getter;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Task;

import java.io.Serializable;
import java.net.Socket;
import java.util.*;

import static java.lang.String.format;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_SLEEP;

/** Основной интерфейс для всех умных устройств. */
public interface ISmartDevice  extends ITaskProvider, IModesProvider, Runnable
{

    /** Запрос на отключение устройства, которое, возможно, занято какой-то операцией. */
    boolean isItSafeToTurnOff ();
    //boolean canSleep ();
    //boolean canBeMaster ();
    //boolean canBeSlave ();
    //void setPort (Port port); < не нужно уже
    //void sleepSwitch (boolean sleep);   //< перевод УУ в режим минимального энергопотребления
    //UUID uuid ();
}
/**  */
