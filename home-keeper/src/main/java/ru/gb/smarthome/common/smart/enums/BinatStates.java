package ru.gb.smarthome.common.smart.enums;

public enum BinatStates {

     BS_ (false, "")
    ,BS_CONTRACT      (false, "Запрос")
    ,BS_ERROR         ( true, "Ошибка")
    ,BS_NOT_SUPPORTED ( true, "Не поддерживается")
    ,BS_REJECTED      ( true, "?")
    ;

/** Помечает коды состояний, которые являются индикаторами ошибки {…}. Например,
 клиент с пом. BinatStates-кодов может информирвать хэндлер о {…}, а
 хэндлеру достаточно проверить истинность поля BinatStates.launchingError полученного кода, чтобы понять,
 составлять сообщение об ошибке, или нет. */
    public final boolean bindingError;

/** Короткое описание элемента BinatStates. */
    public final String  bsName;

    BinatStates (boolean bindError, String bsN) {
        bindingError = bindError;
        bsName = bsN;
    }

/** Количество элементов в BinatStates. */
    public static final int length = values().length;
}
