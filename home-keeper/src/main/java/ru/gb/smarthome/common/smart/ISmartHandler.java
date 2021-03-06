package ru.gb.smarthome.common.smart;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Message;

import java.util.UUID;

public interface ISmartHandler extends ISmartDevice
{

/** Переводим УУ в активное/неактивное состояние.
@param value принимает значения ACTIVE и NOT_ACTIVE.
@return TRUE, если устройство переведено в указанное состочние или уже находится в указаном состоянии. */
    boolean activate (boolean value);  //< переключение активного состояния в УУ

/** Геттер на ClientHandler.active. */
    boolean isActive ();

    UUID getUuid();

/** Сеттер на поле deviceFriendlyName. Юзер может присвоить УУ удобное название. Этот метод имеет смысл использовать только на стороне УД. */
    void setDeviceFriendlyName (String name);

/** Геттер на поле deviceFriendlyName. */
    String getDeviceFriendlyName ();

/** Сеттер на интервал между попытками хэндлера извлечь все задания из очереди. */
    void setPollInterval (int seconds);

/** Запрашиваем у УУ его возможности. На стороне УУ эта информация должна генерироваться
единажды при его инициализации и не должна меняться от запроса к запросу. */
    @NotNull Abilities getAbilities ();

    /** Геттер на текущее состояние УУ. */
    @NotNull DeviceState getState ();

    /** Поместить запрос в очередь запросов. Чем выще приоритет запрошеной операции, тем
    быстрее запрос будет обработан. Коды операций в {@link ru.gb.smarthome.common.smart.enums.OperationCodes
    OperationCodes} выстроены в порядке увеличения приортета. */
    boolean offerRequest (Message mRequest);

}
