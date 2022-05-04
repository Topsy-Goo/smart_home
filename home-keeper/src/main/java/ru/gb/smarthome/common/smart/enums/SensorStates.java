package ru.gb.smarthome.common.smart.enums;

public enum SensorStates
{
     SST_OFF    (false, false)
    ,SST_ON     ( true, false)
    ,SST_ALARM  ( true, true )
    ;//           on    alarm

    public final boolean on, alarm;

    SensorStates (boolean onState, boolean alarmSate) {
        on = onState;
        alarm = alarmSate;
    }

    public static SensorStates get (boolean on, boolean alarm) {
        return on ? (alarm ? SST_ALARM : SST_ON) : SST_OFF;
    }

/** Логика переключения датчика из одного состояния в другое. Метод определяет, допустимо ли
 переключение из состояния from в состояние to.
 @param from Исходное состояние.
 @param to Желаемое состояние.
 @return to, если переключение допустимо, или from, если текущее состояние датчика не позволяет выполнить переключение. */
    public static SensorStates calcState (SensorStates from, SensorStates to)
    {
        return (!from.on && to.alarm) ? from : to;
        // нельзя включить тревожное состояние у выключенного датчика.
        // все остальные переключения допустимы.
    }
}
