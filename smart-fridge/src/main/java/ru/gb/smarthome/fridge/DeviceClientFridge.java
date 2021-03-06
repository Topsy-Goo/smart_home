package ru.gb.smarthome.fridge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.empty.client.DeviceClientEmpty;

import javax.annotation.PostConstruct;
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
                CANNOT_SLEEP)
                .setTasks (propMan.getAvailableTasks())
                .setSensors (propMan.getAvailableSensors())
                .setSlaveTypes (propMan.slaveTypes())
                ;
        sensorsNumber = abilities.getSensors().size();

        taskExecutorService = Executors.newSingleThreadExecutor (r->{
                        Thread t = new Thread (r);
                        t.setDaemon (true);
                        return t;});
        //if (DEBUG) {
            Thread threadConsole = new Thread (()->IConsolReader.runConsole (this));
            threadConsole.setDaemon(true);
            threadConsole.start();
        //}
    }
}
