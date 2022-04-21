package ru.gb.smarthome.washer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.empty.DeviceClientEmpty;

@Component
@Scope ("prototype")
public class DeviceClientWasher extends DeviceClientEmpty
{
    private final PropertyManagerWasher propMan;

    @Autowired
    public DeviceClientWasher (PropertyManagerWasher pmw) {
        super(pmw);
        propMan = pmw;
    }

    @Override public void run () {
    }
}
