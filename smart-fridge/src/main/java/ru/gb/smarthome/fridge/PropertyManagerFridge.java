package ru.gb.smarthome.fridge;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.AUTONOMIC;
import static ru.gb.smarthome.common.FactoryCommon.NON_INTERRUPTIBLE;

@Component("fridge_propman")
@Scope ("singleton")
public class PropertyManagerFridge extends PropertyManagerEmpty
{
    //@Value ("${fridge.name}") private String name;
    //@Value ("${fridge.uuid}") private UUID uuid;
    private final String name = "ATLANT ХМ 4214-000";
    private final UUID      uuid  = UUID.fromString ("429c7bd1-b071-4998-8709-7c6e7ae6e3ab");
    private final Set<Task> tasks = emptyTaskList();

    @PostConstruct public void init() {
        tasks.add (new Task ("Разморозка", AUTONOMIC, 20, TimeUnit.MINUTES, NON_INTERRUPTIBLE));
    }

    public void shutdown() {}

    @Override public UUID getUuid () { return uuid; }
    @Override public String getName () { return name; }
    @Override public Set<Task> getAvailableTasks () {  return tasks;  }
}
