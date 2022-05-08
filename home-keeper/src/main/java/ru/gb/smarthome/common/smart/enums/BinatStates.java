package ru.gb.smarthome.common.smart.enums;

public enum BinatStates {

    //BS_SIGNAL        (false, "Сигнал"),             //< TODO:не используется
    BS_CONTRACT      (false, "Контракт"),
    //BS_ERROR         ( true, "Ошибка"),             //< TODO:не используется
    //BS_NOT_SUPPORTED ( true, "Не поддерживается"),  //< TODO:не используется
    //BS_REJECTED      ( true, "?")                   //< TODO:не используется
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
