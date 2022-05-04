package ru.gb.smarthome.empty;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.PropertyManager;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Component ("empty_propman")
@Scope ("singleton")
public class PropertyManagerEmpty extends PropertyManager
{
    private final String name = "Учебное УУ № 0000";
    private final UUID   uuid = UUID.fromString("bc852471-11e4-4ae4-98c2-4e48a859259f");

    @PostConstruct public void init() {}

    @Override public UUID getUuid ()   { return uuid; }
    @Override public String getName () { return name; }
}
//прародитель всех УУ