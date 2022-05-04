package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.SensorStates;

import java.io.Serializable;
import java.util.*;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.isStringsValid;

/** Состояние УУ для отображения в web-интерфейсе. <p>
На стороне УУ (т.е. в клиенте) рекомендуется эту структуру своевременно заполнять, чтобы на момент
прихода очередного запроса CMD_STATE можно было быстро передать её в УД, т.с. кусочком.
<p>
    На стороне УД (т.е. в хэндлере) эту структуру изменять должен только хэндлер и только те поля,
которые необходимо обновлять для поддержания структуры в актуальном состоянии. Всем остальным
объектам на стороне УД рекомендуется эту структуру только читать.
 */
public class DeviceState implements Serializable
{
    /** Код текущего состояния УУ. */
    @Getter private OperationCodes opCode;

    /** Код ошибки. Используется только при code == CMD_ERROR. */
    @Getter private String errCode;

    /** Текущая операция. Может быть null. */
    @Getter private Task currentTask;

/** Датчики, которыми располагает УУ и состоянием которых оно готово делиться с хэндлером.  */
    @Getter private Map<UUID, SensorStates> sensors = Collections.emptyMap();


/**
@param cod кодтекущего состояния УУ : режим, выполняемыя задача, … (см. {@link ru.gb.smarthome.common.smart.enums.OperationCodes OperationCodes})
@param errCod содержит уточняющий код ошибки, если состояние УУ имеет код CMD_ERROR. Может принимать любые <u>КОРОТКИЕ</u> строковые значения.
 */
    public DeviceState (@NotNull OperationCodes cod, String errCod) {
        opCode = cod;
        setErrCode (errCod);
    }
    public DeviceState () {} //< требование сериализации. Также используется в «билдерах».

/** Делаем максимально полную копию экземпляра, чтобы владелец копии мог работать с ней, не боясь повредить
 данные в оригинале. */
    public DeviceState safeCopy () {
        Task t = currentTask;
        if (t != null)
            t = currentTask.safeCopy();
        return new DeviceState (opCode, errCode)
                        .setCurrentTask (t)
                        .setSensors (new HashMap<>(sensors)/*Collections.unmodifiableMap (sensors)*/);
    }

    public DeviceState setOpCode (@NotNull OperationCodes val) { opCode = val;    return this; }
    public DeviceState setCurrentTask (Task val)    { currentTask = val;  return this; }
    public DeviceState setErrCode (String val) {
        errCode = (val == null) ? "" : val.trim();
        return this;
    }
    public DeviceState setSensors (Map<UUID, SensorStates> val) {
        sensors = (val != null) ? val : Collections.emptyMap();
        return this;
    }

    @Override public String toString ()
    {
        StringBuilder sb = new StringBuilder("датчики: ");
        for (SensorStates v : sensors.values()) {
            sb.append (v.name()).append(" • ");
        }
        return format ("%s%s,\n\tзадача:%s\n\t%s",
                      opCode.name(),
                      isStringsValid (errCode) ? format("(%s)", errCode) : "",
                      currentTask,
                      sb.toString());
    }
}
//@param activ отображает текущее состояние активности УУ : ACTIVE или NOT_ACTIVE .