package ru.gb.smarthome.common.smart.enums;

public enum TaskStates {
     TS_IDLE         (true,  "Бездействует")  //< не выполняется (умолчальное значение; задача может быть запущена)
    ,TS_DONE         (true,  "Готово")  //< выполнено
    ,TS_LAUNCHING    (false, "Запуск…") //< готовится к запуску
    ,TS_RUNNING      (false, "Выполняется")
    ,TS_NEED_SERVICE (false, "Ждёт обслуживания") //< выполняется, но требуется участие юзера для продолжение выполнения
    ,TS_ERROR        (false, "Ошибка. Прервано.") //< неустранимая ошибка (задача не может продолжиться или быть запущена)
    ,TS_NOT_SUPPORTED(true,  "Не поддерживается")
    ,TS_REJECTED     (true,  "Запуск невозможен")  //< отказано в выполнении задачи
    ;
    /** Указывает, можно ли запустить задачу (любую), если текущая задача имеет состояние TaskStates.
    Например, если текущая задача находится в состоянии IDLE или DONE, то её можно перезапустить или
    заменить другой задачей. */
    public final boolean canReplace;
    /** Короткое описание состояния задачи. */
    public final String  tsName;
    /** Количество элементов в TaskStates. */
    public static final int length = values().length;

    TaskStates (boolean canL, String tsN) {
        canReplace = canL;
        tsName = tsN;
    }

}
