package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.enums.TaskStates;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.TaskStates.TS_IDLE;

public class Task implements Serializable
{
    /** <u>короткое</u> название операции. На странице в списке доступных операций будет это название. */
    @Getter private String name;

    /** операция может продолжаться даже при отключении УУ от УД. */
    @Getter private boolean autonomic;

    /** выполнение задачи можно прервать без ущерба для результата */
    @Getter private boolean interruptible;

    /** продолжительность операции */
    @Getter private long duration;

    /** осталось времени до конца операции. На стороне клиента это значение будет обновляться в реальном времени. */
    @Getter private AtomicLong remained;

    /** осталось времени до конца операции. На стороне клиента это значение будет обновляться в реальном времени. */
    @Getter private AtomicLong elapsed;

    @Getter private AtomicReference<TaskStates> tstate;

    @Getter private AtomicReference<String> message;


/**
@param taskName <u>короткое</u> название операции.
@param isAutonomic операция может продолжаться даже при отключении УУ от УД.
@param durationInSeconds продолжительность операции с секундах.
*/
    public Task (String taskName, boolean isAutonomic, long durationInSeconds, boolean isInterruptible)
    {
        if (durationInSeconds < 0)
            throw new IllegalArgumentException();

        name = (taskName == null || taskName.isBlank()) ? DEF_TASK_NAME : taskName.trim();
        autonomic     = isAutonomic;
        duration      = durationInSeconds;
        interruptible = isInterruptible;
        remained      = new AtomicLong (duration);
        elapsed       = new AtomicLong (0);
        tstate        = new AtomicReference<> (DEF_TASK_STATE);
        message       = new AtomicReference<> (DEF_TASK_MESSAGE);
    }

/**
@param taskName <u>короткое</u> название операции.
@param isAutonomic операция может продолжаться даже при отключении УУ от УД.
@param duration продолжительность операции.
@param timeUnits единицы времени для параметра duration.
@param isInterruptible выполнение задачи можно прервать без ущерба для результата
*/
    public Task (String taskName, boolean isAutonomic, long duration, @NotNull TimeUnit timeUnits,
                 boolean isInterruptible)
    {
        this (taskName, isAutonomic, timeUnits.toSeconds(duration), isInterruptible);
    }

    private Task () {} //< требование сериализации.

    /** Конструктор для экземпляров Task, назначение которых — только информирование. */
    public Task (@NotNull String tname, TaskStates ts, String tmessage) {
        name     = tname;
        tstate   = new AtomicReference<>(ts);
        message  = new AtomicReference<>(tmessage != null ? tmessage : DEF_TASK_MESSAGE);
        remained = new AtomicLong(0L);
        elapsed  = new AtomicLong(0L);
        tstate   = new AtomicReference<>(DEF_TASK_STATE);
    }

/** Делаем максимально полную копию экземпляра, чтобы владелец копии мог работать с ней, не боясь повредить
 данные в оригинале */
    public Task safeCopy () {
        Task t = new Task (name, autonomic, duration, interruptible);
        long rem = remained.get(); //TODO: NullPointerException, если задачу прервать ошибкой, а птом «починить» командой /err.
        t.remained = new AtomicLong (rem);
        t.elapsed = new AtomicLong(duration - rem);
        t.tstate = new AtomicReference<>(tstate.get());
        t.message = new AtomicReference<>(message.get());
        return t;
    }

    public Task setName (@NotNull String val) { name = val;   return this; }
    public Task setTstate (@NotNull TaskStates val) { tstate.set(val);   return this; }
    public Task setMessage (@NotNull String val) { message.set(val);   return this; }

/** Уменьшаем remained, увеличиваем elapsed.
@param delta шаг. */
    public void tick (long delta) {
        long rem = Math.max(0, remained.get() - delta);
        remained.set(rem);
        elapsed.set(duration - rem);
    }

    @Override public String toString () {
        return format ("Task[%s: %d/%d сек, %s%s | %s | %s]",
                        name
                       ,elapsed.get()    ,duration
                       ,autonomic ? "A" : "a"   ,interruptible ? "I" : "i"
                       ,tstate.get().name()
                       ,message.get()
                       );
    }

/** Сравнение задач выполняется только по их имени. Основание — юзер будет выбирать задачи по их имени,
и для него одинаковые задачи сразными именами всегад будут разными задачами, и остальные парамтеры
проверять нет смысла. */
    @Override public boolean equals (Object o)
    {
        if (this == o) return true;

        if (o instanceof Task)
            o = ((Task)o).name;

        if (o instanceof String)
            return name.equals(o);

        return false;
    }
}
//@param cod код операции (см. {@link ru.gb.smarthome.common.smart.enums.OperationCodes OperationCodes}).