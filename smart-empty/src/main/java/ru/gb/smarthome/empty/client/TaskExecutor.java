package ru.gb.smarthome.empty.client;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.printf;
import static ru.gb.smarthome.common.smart.enums.TaskStates.*;
import static ru.gb.smarthome.empty.EmptyApp.DEBUG;

/** Этот класс изображает запущенную на исполнение задачу. Всё, что он делает, — это
отсчёт времени до окончания задачи. По истечении этого времени call() возвращает true.
Если во время «операции» произошла одибка, то call() возвращает false.
<p>
    Во время выполнения задачи в структуру state должны вноситься минимальные изменения,
чтобы было легче синхронизировать их с передачей state в УД. Нельзя заменять структуры, например,
state, state.currentTask, но можно изменять их Atomic-поля. Сейчас эти изменения следующие:<br>
• в state.currentTask меняем Atomic-поля remained и elapsed — они обновляются так, чтобы отображать
 прогресс операции.<br>
 • ;<br>
 • ;<br>
 <p>
Если задача завершилась ошибкой (Task.tstate == TS_ERROR), то мы НЕ считаем это
ошибкой всего УУ и не устанавливаем state.code == CMD_ERROR.
 */
public class TaskExecutor implements Callable<Boolean>
{
    private final Task   theTask;
    private final String taskName;

    public TaskExecutor (@NotNull Task newTask) {
        theTask = newTask.setTstate (TS_LAUNCHING)
                         .setMessage (format ("Запускается задача «%s».", newTask.getName()));
        taskName = newTask.getName();
    }

    @Override
    public Boolean call () {
        if (DEBUG) printf ("\nTaskExecutor.call(): начинает работать задача: %s.\n", theTask);

        theTask.setTstate (TS_RUNNING).setMessage (format ("Выполняется задача «%s»", taskName));

        boolean ok = false;
        try {
            AtomicLong reminder = theTask.getRemained();
            while (reminder.get() > 0L)
            {
                TimeUnit.SECONDS.sleep(1);
                theTask.tick(1);
            }
            ok = reminder.get() == 0L;
            theTask.setTstate (ok ? TS_DONE : TS_ERROR).setMessage ("Задача завершена.");
        }
        catch (Exception e) {
            ok = (e instanceof InterruptedException && theTask.isInterruptible());
            if (ok)
                theTask.setTstate (TS_INTERRUPTED).setMessage ("Задача прервана.");
            else
                theTask.setTstate (TS_ERROR).setMessage ("Задача прервана из-за ошибки.");
        }
        finally {
            if (DEBUG) printf ("\nTaskExecutor.call().finally: Завершилась задача: %s.", theTask);
        }
        return ok;
    }
}
