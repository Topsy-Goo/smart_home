package ru.gb.smarthome.sequrity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.empty.complex.DevClientEmptyComplex;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import static ru.gb.smarthome.common.FactoryCommon.CANNOT_SLEEP;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SEQURITY_CONTROLLLER;

@Component
@Scope ("prototype")
public class DevClientSequrController extends DevClientEmptyComplex
{

    @Autowired
    public DevClientSequrController (PropManagerSequrController pmsc) {
        super(pmsc);
    }

    @PostConstruct
    @Override public void init ()
    {
        abilities = new Abilities(
                SEQURITY_CONTROLLLER,
                propMan.getName(),
                propMan.getUuid(),
                CANNOT_SLEEP)
                .setSlave (true)
                .setTasks ((propMan.getAvailableTasks()))
                .setSensors (propMan.getAvailableSensors())
                .setSlaveTypes(propMan.slaveTypes())
                ;

        sensorsNumber = abilities.getSensors().size();
        initSensors();

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
