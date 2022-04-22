package ru.gb.smarthome.homekeeper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.structures.Task;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class PropertyManagerHome extends PropertyManager
{
    //@Value ("${home.uuid}") private UUID uuid;
    private final String name = "Умный дом 1";
    private final UUID      uuid  = UUID.fromString ("7db3548c-717f-49e0-b8a4-137703f19496");
    private final Set<Task> tasks = emptyTaskList();

    @PostConstruct public void init() {}

    public void shutdown() {}

    @Override public UUID getUuid () { return uuid; }
    @Override public String getName () { return name; }
    @Override public Set<Task> getAvailableTasks () {  return tasks;  }
}
