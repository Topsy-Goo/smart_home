package ru.gb.smarthome.common.smart.enums;

import java.util.Arrays;

import static ru.gb.smarthome.common.FactoryCommon.isStringsValid;

public enum TaskStates
{
    //,TS_NONE         ( true, false,  true, "Нет задач")
     TS_IDLE         ( true, false, false, "Бездействует")  //< не выполняется (умолчальное значение; задача может быть запущена)
    ,TS_DONE         ( true, false, false, "Готово")  //< выполнено
    ,TS_LAUNCHING    (false,  true, false, "Запуск…") //< готовится к запуску
    ,TS_RUNNING      (false,  true, false, "Выполняется")
    ,TS_NEED_SERVICE (false,  true, false, "Ждёт обслуживания") //< выполняется, но требуется участие юзера для продолжение выполнения
    ,TS_INTERRUPTED  ( true, false, false, "Выполнение прервано") //< прерываемая задача остановлена (прерываемая == может быть остановлена в любое время; если прерывается НЕпрерываемая задача, то устройство имеет право войти в состояние ошибки.)
    ,TS_ERROR        (false, false,  true, "Ошибка.") //< неустранимая ошибка (задача не может продолжиться)
    ,TS_NOT_SUPPORTED( true, false,  true, "Не поддерживается")
    ,TS_REJECTED     ( true, false,  true, "Запуск невозможен")  //< отказано в выполнении задачи
    ;//               repl   runn    lerr   tsNm
/** Количество элементов в TaskStates. */
    public static final int length = values().length;

    /** Указывает, можно ли запустить задачу (любую), если текущая задача имеет состояние TaskStates.
    Например, если текущая задача находится в состоянии IDLE или DONE, то её можно перезапустить или
    заменить другой задачей. */
    public final boolean canReplace;

/** TRUE — Задача выполняется. Также это состояние может соответствовать задаче, которая приостановлена,
 чтобы продолжиться после выполнения какого-то условия. (Например, стиралка приостановила работу, вывесила
 состояние TS_NEED_SERVICE и ждёт, когда юзер заберёт выстиранное бельё.)<p>
 FALSE — Задача не выполняется (завершилась, или прервана без возможности её продолжить). Состояние задачи
 допускает её перезапуск, если этому не мешают другие обстоятельства.  */
    public final boolean runningState;

/** Помечает коды состояний, которые являются индикаторами ошибки запуска задачи. Например,
 клиент с пом. TaskStates-кодов может информирвать хэндлер о состоянии запуска задачи, а
 хэндлеру достаточно проверить истинность поля TaskStates.launchingError полученного кода,
 чтобы понять, составлять сообщение об ошибке, или нет.  */
    public final boolean launchingError;

/** Короткое описание состояния задачи. */
    public final String  tsName;

    TaskStates (boolean canreplace, boolean runnState, boolean launchErr, String tsN) {
        canReplace = canreplace;
        runningState = runnState;
        launchingError = launchErr;
        tsName = tsN;
    }

    public static TaskStates byTsName (String tsname)
    {
        if (!isStringsValid (tsname))
            return null;
        return Arrays.stream (values())
                     .filter (ts->ts.tsName.equals(tsname))
                     .findFirst()
                     .orElse (null);
    }
}
