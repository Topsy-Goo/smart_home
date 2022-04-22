package ru.gb.smarthome.empty;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.structures.Task;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Component ("empty_propman")
@Scope ("singleton")
public class PropertyManagerEmpty extends PropertyManager
{
    protected final Random rnd = new Random();
    private   final String name = "Учебное УУ №" + rnd.nextInt(100500);
    private   final UUID uuid = UUID.fromString("bc852471-11e4-4ae4-98c2-4e48a859259f");
    private   final Set<Task> tasks = emptyTaskList();

    @PostConstruct public void init() {}

    public void shutdown() {}

    @Override public UUID getUuid () { return uuid; }
    @Override public String getName () { return name; }
    @Override public Set<Task> getAvailableTasks () {  return tasks;  }
}
