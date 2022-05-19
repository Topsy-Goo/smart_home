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

public class Task implements Serializable
{
    /** <u>короткое</u> название операции. На странице в списке доступных операций будет это название. */
    @Getter private String name;

    /** операция может продолжаться даже при отключении УУ от УД. */
    @Getter private boolean autonomic;

/** выполнение задачи можно прервать без ущерба для результата.<p>
 Юзер не должен иметь возможность пользоваться этим флагом — запуском одной задачи прерывать
 выполнение другой задачи, — пусть остановит текущую, и запустит желаемую.<p>
 Этот флаг используется только для сигналов, когда по контракту УУ должно запустить задачу
 как ответ на сигнал, — в этом случае interruptible-задача должна быть прервана и уступить
 место контрактной задаче. */
    @Getter private boolean interruptible;

    /** продолжительность операции */
    @Getter private long duration;

    /** осталось времени до конца операции. На стороне клиента это значение будет обновляться в реальном времени. */
    @Getter private AtomicLong remained;

    /** осталось времени до конца операции. На стороне клиента это значение будет обновляться в реальном времени. */
    @Getter private AtomicLong elapsed;

    private AtomicReference<TaskStates> tstate;

/** Короткое сообщение без символов \n. */
    private AtomicReference<String> message;


/**
@param taskName <u>короткое</u> название операции.
@param isAutonomic операция может продолжаться даже при отключении УУ от УД.
@param durationInSeconds продолжительность операции с секундах.
*/
    public Task (String taskName, boolean isAutonomic, boolean isInterruptible, long durationInSeconds)
    {
        if (durationInSeconds < 0)
            throw new IllegalArgumentException();

        name = (taskName == null || taskName.isBlank()) ? DEF_TASK_NAME : taskName.trim();
        autonomic     = isAutonomic;
        duration      = durationInSeconds;
        interruptible = isInterruptible;
        remained      = new AtomicLong (duration);
        elapsed       = new AtomicLong (0L);
        tstate        = new AtomicReference<> (DEF_TASK_STATE);
        message       = new AtomicReference<> (DEF_TASK_MESSAGE);
    }

/** Конструктор с возможностью указать любую единицу измерения для duration, — конструктор
 сам переведёт её в секунды.
 @param taskName <u>короткое</u> название операции.
 @param isAutonomic операция может продолжаться даже при отключении УУ от УД.
 @param duration продолжительность операции.
 @param timeUnits единицы времени для параметра duration.
 @param isInterruptible выполнение задачи можно прервать без ущерба для результата
*/
    public Task (String taskName, boolean isAutonomic, boolean isInterruptible,
                 long duration, @NotNull TimeUnit timeUnits)
    {
        this (taskName, isAutonomic, isInterruptible, timeUnits.toSeconds(duration));
    }

    private Task () {} //< требование сериализации.

/** Конструктор для «облегчённых» экземпляров Task, назначение которых — только информирование. */
    public Task (@NotNull String tname, TaskStates ts, String tmessage) {
        name     = tname;
        tstate   = new AtomicReference<>(ts != null ? ts : DEF_TASK_STATE);
        message  = new AtomicReference<>(tmessage != null ? tmessage : DEF_TASK_MESSAGE);
        //remained = new AtomicLong(0L);
        //elapsed  = new AtomicLong(0L);
    }

/** Делаем максимально полную копию экземпляра, чтобы владелец копии мог работать с ней, не боясь повредить
 данные в оригинале */
    public static Task safeCopy (Task t)
    {
        if (t == null)
            return null;
        Task tnew = new Task (t.name, t.autonomic, t.interruptible, t.duration);
        long rem = t.remained.get();
        tnew.remained = new AtomicLong (rem);
        tnew.elapsed  = new AtomicLong (tnew.duration - rem);
        tnew.tstate   = new AtomicReference<> (t.tstate.get());
        tnew.message  = new AtomicReference<> (t.message.get());
        return tnew;
    }

    public Task setName (@NotNull String val) { name = val;   return this; }
    public Task setTstate (@NotNull TaskStates val) { tstate.set(val);   return this; }
    public Task setMessage (@NotNull String val) { message.set(val);   return this; }

    public TaskStates getTstate ()  { return tstate.get(); }
    public String     getMessage () { return message.get(); }

/** Уменьшаем remained, увеличиваем elapsed.
@param delta шаг. */
    public void tick (long delta) {
        long rem = Math.max(0, remained.get() - delta);
        remained.set(rem);
        elapsed.set(duration - rem);
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

    @Override public String toString () {
        return format ("[%s: %d/%d сек, %s%s | %s | %s]",
                        name
                       ,elapsed.get()    ,duration
                       ,autonomic ? "A" : "a"   ,interruptible ? "I" : "i"
                       ,tstate.get().name()
                       ,message.get()
                       );
    }
}
