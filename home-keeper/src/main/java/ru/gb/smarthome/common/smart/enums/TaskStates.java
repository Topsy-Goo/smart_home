package ru.gb.smarthome.common.smart.enums;

public enum TaskStates {
     TS_IDLE         (true)  //< не выполняется (умолчальное значение; задача может быть запущена)
    ,TS_DONE         (true)  //< выполнено
    ,TS_LAUNCHING    (false) //< готовится к запуску
    ,TS_RUNNING      (false) //< выполняется
    ,TS_NEED_SERVICE (false) //< выполняется, но требуется участие юзера для продолжение выполнения
    ,TS_ERROR        (false) //< неустранимая ошибка (задача не может продолжиться или быть запущена)
    ,TS_NOT_SUPPORTED(true)  //< не поддерживается
    ,TS_REJECTED     (true)  //< отказано в выполнении задачи
    ;
    /** Указывает, можно ли запустить задачу (любую), если текущая задача имеет состояние TaskStates.
    Например, если текущая задача находится в состоянии IDLE или DONE, то её можно перезапустить или
    заменить другой задачей. */
    public final boolean canReplace;

    TaskStates (boolean canL) { canReplace = canL; }
}
