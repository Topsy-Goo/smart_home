package ru.gb.smarthome.common.smart;


/** Основной интерфейс для всех умных устройств. */
public interface ISmartDevice extends Runnable
{

    /* * Запрос на отключение устройства, которое, возможно, занято какой-то операцией. */
    //boolean isItSafeToTurnOff ();

    //boolean canSleep ();
    //boolean canBeMaster ();
    //boolean canBeSlave ();
    //void setPort (Port port); < не нужно уже
    //void sleepSwitch (boolean sleep);   //< перевод УУ в режим минимального энергопотребления
    //UUID uuid ();
}
