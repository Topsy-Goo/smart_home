package ru.gb.smarthome.weatherstation;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.empty.client.DeviceClientEmpty;

import javax.annotation.PostConstruct;

import java.util.concurrent.Executors;

import static ru.gb.smarthome.common.FactoryCommon.CANNOT_SLEEP;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.FRIDGE;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.METEO_STATION;

@Component
@Scope ("prototype")
public class DeviceClientWeatherStation extends DeviceClientEmpty
{
    //private final PropertyManagerWeatherStation propMan;

    public DeviceClientWeatherStation (PropertyManagerWeatherStation pmws) {
        super(pmws);
    }

    @PostConstruct
    public void init ()
    {
        abilities = new Abilities(
                METEO_STATION,
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
        taskExecutorService.execute (this::workHard);
    }

/** Run-метод потока, ответственного за полуение сведений о погоде из метеослужбы. */
    private void workHard ()
    {
        //TODO: Нам здесь понадобятся координаты, которые юзер введёт на фронте.
    }
}
