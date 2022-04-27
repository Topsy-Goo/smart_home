package ru.gb.smarthome.washer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Component ("washer_propman")
@Scope ("singleton")
public class PropertyManagerWasher extends PropertyManagerEmpty
{
    private final String name = "Indesit IWUB 4085 (CIS)";
    private final UUID   uuid = UUID.fromString("8d6ead41-d76e-4bcf-8769-0434ce6a0998");
    private final Set<Task>   tasks = emptyTaskList();


    @PostConstruct public void init() {
        tasks.add (new Task ("Быстрая стирка",    AUTONOMIC, 20, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        tasks.add (new Task ("Стирка джинсов",    AUTONOMIC, 25, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        tasks.add (new Task ("Деликатная стирка", AUTONOMIC, 30, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        tasks.add (new Task ("Ночная стирка",     AUTONOMIC, 45, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        tasks.add (new Task ("Замачивание",       AUTONOMIC, 60, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        tasks.add (new Task ("Отжим",             AUTONOMIC, 15, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        tasks.add (new Task ("Полоскание",        AUTONOMIC, 10, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        //println ("\n************************* Считывание настроек: *************************");
        //println ("************************** Настройки считаны: **************************");
    }

    public void shutdown() {}

    @Override public UUID getUuid ()                { return uuid; }
    @Override public String getName ()              { return name; }
    @Override public Set<Task> getAvailableTasks () {  return tasks;  }
}