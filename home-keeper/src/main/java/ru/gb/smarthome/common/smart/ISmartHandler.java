package ru.gb.smarthome.common.smart;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.Binate;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.homekeeper.dtos.SensorDto;

import java.util.List;

public interface ISmartHandler extends ISmartDevice
{

/** Переводим УУ в активное/неактивное состояние.
@param value принимает значения ACTIVE и NOT_ACTIVE.
@return TRUE, если устройство переведено в указанное состочние или уже находится в указаном состоянии. */
    boolean activate (boolean value);  //< переключение активного состояния в УУ

/** Геттер на ClientHandler.active. */
    boolean isActive ();

/** Сеттер на поле deviceFriendlyName. Юзер может присвоить УУ удобное название. Этот метод имеет смысл использовать только на стороне УД. */
    boolean setDeviceFriendlyName (String name);

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

/** Собираем собщения, которые хэндлер успел собрать для юзера. */
    List<String> getLastNews ();

/** Реагируя на этот вызов, хэндлер должен обработтать запрос на связывание своего
 подопечного УУ с другим УУ. (На самом деле связывание происходит между хэндлерами,
 а сами УУ ничего про связывание не знают.)
 @param binate содержит необходимую инфорацию для связывания. */
    void pair (Binate binate);

/* * Отдаём состояния датчиков, в каком бы состоянии они не были: OFF/ON/ALARM. */
    //List<SensorDto> reportSensorsState ();
}
