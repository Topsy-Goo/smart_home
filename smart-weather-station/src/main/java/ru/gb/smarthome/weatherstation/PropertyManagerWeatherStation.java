package ru.gb.smarthome.weatherstation;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.util.UUID;

import static ru.gb.smarthome.common.FactoryCommon.uuidFromString;

@Component ("fridge_propman")
@Scope ("singleton")
public class PropertyManagerWeatherStation extends PropertyManagerEmpty
{
    private final String name = "Метео-Р 216 (ТА)";
    private final UUID   uuid = uuidFromString ("55a17017-508f-454f-8015-a4cc62403671");

    @PostConstruct
    @Override public void init() {}

    @Override public UUID getUuid ()   { return uuid; }
    @Override public String getName () { return name; }
}
