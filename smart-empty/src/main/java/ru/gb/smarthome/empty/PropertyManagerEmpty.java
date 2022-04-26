package ru.gb.smarthome.empty;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.structures.Task;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.UUID;

@Component ("empty_propman")
@Scope ("singleton")
public class PropertyManagerEmpty extends PropertyManager
{
    private final String name = "Учебное УУ № 0000";
    private final UUID   uuid = UUID.fromString("bc852471-11e4-4ae4-98c2-4e48a859259f");
    private final Set<Task> tasks = emptyTaskList();


    @PostConstruct public void init() {
/*        println ("\n************************* Считывание настроек: *************************");
        vendorString = environment.getProperty ("smart.vendor.string", String.class,
                                                "Учебное УУ № 0000");
        println ("smart.vendor.string: " + vendorString);
        uuid = UUID.fromString (environment.getProperty ("smart.vendor.uuid", String.class,
                                                         "1cc2e189-72ce-47a4-990c-57d3e2d4e9c0"));
        println ("smart.vendor.uuid: " + uuid);
        println ("************************** Настройки считаны: **************************\n");*/
    }

    public void shutdown() {}

    @Override public UUID getUuid ()                { return uuid; }
    @Override public String getName ()              { return name; }
    @Override public Set<Task> getAvailableTasks () {  return tasks;  }
}
