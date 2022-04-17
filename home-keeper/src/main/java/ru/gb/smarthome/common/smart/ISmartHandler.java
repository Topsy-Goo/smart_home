package ru.gb.smarthome.common.smart;

import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceState;

public interface ISmartHandler extends ISmartDevice
{
    boolean activate (boolean on);  //< переключение активного состояния в УУ

    boolean setDeviceFriendlyName (String name);

/** Интервал между извлечениями всех заданий из очереди. */
    void setPollInterval (int seconds);

/** Запрашиваем у УУ его возможности. На стороне УУ эта информация должна генерироваться
единажды при его инициализации и не должна меняться от запроса к запросу. */
    Abilities getAbilities ();

/** Запрашиваем у УУ его состояние. На стороне УУ эта информация должна отражать текущее
состояние УУ. */
    DeviceState getState ();

}
