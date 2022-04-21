package ru.gb.smarthome.fridge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.empty.DeviceClientEmpty;

@Component
@Scope ("prototype")
public class DeviceClientFridge extends DeviceClientEmpty
{
    private final PropertyManagerFridge propman;

    @Autowired
    public DeviceClientFridge (PropertyManagerFridge pmf) {
        super(pmf);
        propman = pmf;
    }

    @Override public void run () {
    }

}
