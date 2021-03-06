package ru.gb.smarthome.homekeeper;

import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.PropertyManager;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Component
public class PropertyManagerHome extends PropertyManager
{
    private final String name = "Устройства сгруппированы по типу.";
    private final UUID   uuid = UUID.fromString ("7db3548c-717f-49e0-b8a4-137703f19496");
    private final String aothoKey = "L717F-49E0B8A4-7DB35C-RT7W0-3FP9496";

    @PostConstruct public void init() {}

    public void shutdown() {}

    @Override public UUID getUuid ()   { return uuid; }
    @Override public String getName () { return name; }

    public String getKey () {    return aothoKey;    }
}
