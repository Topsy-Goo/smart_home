package ru.gb.smarthome.fridge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.empty.DeviceClientEmpty;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.FRIDGE;

@Component
@Scope ("prototype")
public class DeviceClientFridge extends DeviceClientEmpty
{

    @Autowired
    public DeviceClientFridge (PropertyManagerFridge pmf) {
        super(pmf);
    }

    @PostConstruct public void init ()
    {
        abilities = new Abilities(
                FRIDGE,
                propMan.getName(),
                propMan.getUuid(),
                new ArrayList<>(propMan.getAvailableTasks()),
                CANNOT_SLEEP);
        exeService = Executors.newSingleThreadExecutor (r->{
                            Thread t = new Thread (r);
                            t.setDaemon (true);
                            return t;
                        });
        //if (DEBUG) {
            Thread threadConsole = new Thread (()->IConsolReader.runConsole (this));
            threadConsole.setDaemon(true);
            threadConsole.start();
        //}
    }

    //@Override public void run () { }
}
