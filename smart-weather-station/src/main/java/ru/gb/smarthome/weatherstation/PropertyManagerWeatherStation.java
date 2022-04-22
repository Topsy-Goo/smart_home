package ru.gb.smarthome.weatherstation;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component ("fridge_propman")
@Scope ("singleton")
public class PropertyManagerWeatherStation extends PropertyManagerEmpty
{
    private final Set<Task> tasks = emptyTaskList();
    private final UUID      uuid  = UUID.fromString ("55a17017-508f-454f-8015-a4cc62403671");
    private final String name = "Метео-Р 216 (ТА)";

    @PostConstruct public void init() {}

    public void shutdown() {}

    @Override public UUID getUuid () { return uuid; }
    @Override public String getName ()               { return name; }
    @Override public Set<Task> getAvailableTasks () {  return tasks;  }
}
