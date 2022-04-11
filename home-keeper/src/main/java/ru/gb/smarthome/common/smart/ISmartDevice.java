package ru.gb.smarthome.common.smart;


import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Port;

import java.util.Map;
import java.util.UUID;

/** Основной интерфейс для всех умных устройств. */
public interface ISmartDevice  extends ITaskProvider, IModesProvider, Runnable
{
    UUID uuid ();  //уникальный UUID умного устройства

//---------- ещё не сделано ------------------------
    DeviceTypes deviceType (); //< тип УУ.
    DeviceStatus status ();

    /** Состояние УУ для печати в web-интерфейсе. Если on==true, то нужно заполнить и statistics.<p>
     statistics — это пары строк в формате Название_параметра: значение. */
    static class DeviceStatus {
        final boolean on; //< true == устройство включено
        final Map<String, String> statistics;
        public DeviceStatus (boolean on, Map<String, String> stat) {
            this.on = on;
            this.statistics = stat;
        }
    }

    //void setPort (Port port); < не нужно уже
    void setFriendlyName (String name);  //< юзер может присвоить устройству название
    void activate (boolean on);  //< переключение активного состояния в УУ
    void sleepSwitch (boolean sleep);   //< перевод УУ в режим минимального энергопотребления

    /** запрос на отключение устройства, которое, возможно, занято какой-то операцией. */
    boolean isItSafeTurnOff ();
    boolean canBeMaster ();
    boolean canBeSlave ();
}
