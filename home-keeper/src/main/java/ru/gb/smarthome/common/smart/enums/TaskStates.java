package ru.gb.smarthome.common.smart.enums;

public enum TaskStates {
     TS_IDLE         (true,  false, false, "Бездействует")  //< не выполняется (умолчальное значение; задача может быть запущена)
    ,TS_DONE         (true,  false, false, "Готово")  //< выполнено
    ,TS_LAUNCHING    (false,  true, false, "Запуск…") //< готовится к запуску
    ,TS_RUNNING      (false,  true, false, "Выполняется")
    ,TS_NEED_SERVICE (false,  true, false, "Ждёт обслуживания") //< выполняется, но требуется участие юзера для продолжение выполнения
    ,TS_INTERRUPTED  (true,  false, false, "Выполнение прервано") //< прерываемая задача остановлена (прерываемая == может быть остановлена в любое время)
    ,TS_ERROR        (false, false,  true, "Ошибка.") //< неустранимая ошибка (задача не может продолжиться)
    ,TS_NOT_SUPPORTED(true,  false,  true, "Не поддерживается")
    ,TS_REJECTED     (true,  false,  true, "Запуск невозможен")  //< отказано в выполнении задачи
    ;
    /** Указывает, можно ли запустить задачу (любую), если текущая задача имеет состояние TaskStates.
    Например, если текущая задача находится в состоянии IDLE или DONE, то её можно перезапустить или
    заменить другой задачей. */
    public final boolean canReplace;

/** TRUE — Задача выполняется или приостановлена, чтобы продолжиться после выполнения какого-то условия.
 Состояние задачи не допускает её перезапуск или остановку.
 <p>
 FALSE — Задача не выполняется (завершилась, или прервана без возможности её продолжить). Состояние задачи
 допускает её перезапуск, если этому не мешают другие обстоятельства.  */
    public final boolean runningState;

/** Коды состояний, которые являются индикаторами ошибки запуска задачи. Например, клиент с пом.этих
 кодов может информирвать хэндлер о том, что послужило препятствием для запуска запрошеной задачи). */
    public final boolean launchingError;

/** Короткое описание состояния задачи. */
    public final String  tsName;

/** Количество элементов в TaskStates. */
    public static final int length = values().length;

    TaskStates (boolean canreplace, boolean runnState, boolean launchErr, String tsN) {
        canReplace = canreplace;
        runningState = runnState;
        launchingError = launchErr;
        tsName = tsN;
    }
    //public boolean greaterThan (TaskStates other) { return (other == null) || this.compareTo (other) > 0; }
    //public boolean lesserThan (TaskStates other) { return (other == null) || this.compareTo (other) < 0; }
}
