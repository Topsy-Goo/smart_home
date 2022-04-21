package ru.gb.smarthome.washer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

@Component ("empty_propman")
@Scope ("singleton")
public class PropertyManagerWasher extends PropertyManagerEmpty
{
    public void init() {
    }

    public void shutdown() {
    }
}
