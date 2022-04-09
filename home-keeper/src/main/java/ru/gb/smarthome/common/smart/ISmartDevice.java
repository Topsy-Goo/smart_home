package ru.gb.smarthome.common.smart;


import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Port;

import java.util.Map;
import java.util.UUID;

/** Основной интерфейс для всех умных устройств. */
public interface ISmartDevice  extends ITaskProvider, IModesProvider, Runnable
{
    DeviceTypes deviceClass(); //< тип УУ.
    UUID uuid ();  //уникальный UUID умного устройства
    DeviceStatus status ();

    /** Состояние УУ для печати в web-интерфейсе. Если on==true, то нужно заполнить и statistics.<p>
     statistics — это пары строк в формате Название_параметра: значение. */
    class DeviceStatus {
        final boolean             on; //< true == устройство включено
        final Map<String, String> statistics;
        public DeviceStatus (boolean on, Map<String, String> stat) {
            this.on = on;
            this.statistics = stat;
        }
    }

    //void setPort (Port port); < не нужно уже
    void setName (String name);  //< юзер может присвоить устройству название
    void turnOn ();  //< перевод УУ в активное состояние
    void turnOff (); //< перевод УУ в НЕактивное состояние
    void sleep ();   //< перевод УУ в режим минимального энергопотребления
    void wakeUp ();  //< вывод УУ из режима минимального энергопотребления

    /** запрос на отключение устройства, которое, возможно, занято какой-то операцией. */
    boolean isItSafeTurnOff ();
    boolean canBeMaster ();
    boolean canBeSlave ();
}
