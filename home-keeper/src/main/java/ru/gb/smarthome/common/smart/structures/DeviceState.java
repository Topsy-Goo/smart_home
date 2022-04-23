package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.enums.OperationCodes;

import java.io.Serializable;

import static java.lang.String.format;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_SLEEP;

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
    /** Указывает, активно ли в данный момент УУ. Варианты значений: ACTIVE и NOT_ACTIVE. */
    @Getter private boolean active;
    /** Код текущего состояния УУ. */
    @Getter private OperationCodes opCode;
    /** Код ошибки. Используется только при code == CMD_ERROR. */
    @Getter private String errCode;
    /** Текущая операция. Может быть null. */
    @Getter private Task currentTask;

    //String[] modeDesciption = new String[2] {"", ""}; <<<<< что-то для этого, кажется, есть в наблосках.
    // * statistics — это пары строк в формате Название_параметра: значение. */
    //private Map<String, String> statistics = new HashMap<>();

/**
@param activ отображает текущее состояние активности УУ : ACTIVE или NOT_ACTIVE .
@param cod кодтекущего состояния УУ : режим, выполняемыя задача, … (см. {@link ru.gb.smarthome.common.smart.enums.OperationCodes OperationCodes})
@param errCod содержит уточняющий код ошибки, если состояние УУ имеет код CMD_ERROR. Может принимать любые <u>КОРОТКИЕ</u> строковые значения.
 */
    public DeviceState (boolean activ, @NotNull OperationCodes cod, String errCod) {
        active = activ;
        opCode = cod;
        setErrCode (errCod);
    }
    public DeviceState () {} //< требование сериализации. Также используется в «билдерах».

/** Делаем максимально полную копию экземпляра, чтобы владелец копии мог работать с ней, не боясь повредить
 данные в оригинале. */
    public DeviceState safeCopy () {
        Task t = currentTask;
        if (t != null) t = currentTask.safeCopy();
        return new DeviceState (active, opCode, errCode).setCurrentTask (t);
    }

/*  public String getErrCode ()      { return errCode; }
    public OperationCodes getCode () { return code; }
    public boolean isActive ()       { return active; }*/

    public DeviceState setOpCode (@NotNull OperationCodes val) { opCode = val;    return this; }
    public DeviceState setActive      (boolean val) { active = val;     return this; }
    public DeviceState setCurrentTask (Task val)    { currentTask = val;  return this; }
    public DeviceState setErrCode (String val) {
        errCode = val == null ? "" : val.trim();
        return this;
    }

    @Override public String toString ()
    {
        return format ("%s%s, %s, %s"
                      ,opCode.name()
                      ,errCode == null || errCode.isBlank() ? "" : format("(%s)", errCode)
                      ,active ? "Активно" : "Неактивно"
                      ,opCode.equals(CMD_SLEEP) ? "Спит" : "Не спит"
                      );
    }
}
