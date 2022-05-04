package ru.gb.smarthome.sequrity.camera;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.empty.complex.DevClientEmptyComplex;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;

import static ru.gb.smarthome.common.FactoryCommon.CANNOT_SLEEP;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SEQURITY_CAMERA;

@Component
@Scope ("prototype")
public class DevClientSequrCamera extends DevClientEmptyComplex
{
    @Autowired
    public DevClientSequrCamera (PropManagerSequrCamera pmsc) {
        super(pmsc);
    }

    @PostConstruct
    @Override public void init ()
    {
        abilities = new Abilities(
                SEQURITY_CAMERA,
                propMan.getName(),
                propMan.getUuid(),
                CANNOT_SLEEP)
                .setMaster (true)
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
