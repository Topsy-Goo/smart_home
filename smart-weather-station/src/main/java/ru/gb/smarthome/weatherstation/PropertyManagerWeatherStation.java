package ru.gb.smarthome.weatherstation;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

@Component ("fridge_propman")
@Scope ("singleton")
public class PropertyManagerWeatherStation extends PropertyManagerEmpty
{
    public void init() {
    }

    public void shutdown() {
    }
}
