package ru.gb.smarthome.sequrity.camera.client;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.client.TaskExecutor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static ru.gb.smarthome.common.FactoryCommon.printf;
import static ru.gb.smarthome.common.smart.enums.TaskStates.*;
import static ru.gb.smarthome.empty.EmptyApp.DEBUG;

public class CameraStreamExecutor extends TaskExecutor
{
    private final SmartCamera smartCamera;

    public CameraStreamExecutor (@NotNull Task newTask, SmartCamera camera) {
        super(newTask);
        smartCamera = camera;
    }

    @Override protected boolean taskBody () //throws InterruptedException
    {
        boolean ok;
        try {
            AtomicLong reminder = theTask.getRemained();
            if (smartCamera.start())
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
            smartCamera.stop();
            if (DEBUG) printf ("\nTaskExecutor.call().finally: Завершилась задача: %s.", theTask);
        }
        return ok;
    }
}
