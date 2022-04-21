package ru.gb.smarthome.weatherstation;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.empty.DeviceClientEmpty;

@Component
@Scope ("prototype")
public class DeviceClientWeatherStation extends DeviceClientEmpty
{
    private final PropertyManagerWeatherStation propMan;

    public DeviceClientWeatherStation (PropertyManagerWeatherStation pmws) {
        super(pmws);
        propMan = pmws;
    }

    @Override public void run () {
    }
}
