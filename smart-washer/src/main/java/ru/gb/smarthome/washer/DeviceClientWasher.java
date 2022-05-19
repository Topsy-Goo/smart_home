package ru.gb.smarthome.washer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.empty.client.DeviceClientEmpty;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;

import static ru.gb.smarthome.common.FactoryCommon.CAN_SLEEP;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.WASHER;

@Component
@Scope ("prototype")
public class DeviceClientWasher extends DeviceClientEmpty
{
    @Autowired
    public DeviceClientWasher (PropertyManagerWasher pmw) {
        super(pmw);
    }

    @PostConstruct public void init ()
    {
        abilities = new Abilities(
                WASHER,
                propMan.getName(),
                propMan.getUuid(),
                CAN_SLEEP)
                .setSlave (true)
                .setTasks (propMan.getAvailableTasks())
                .setSensors (propMan.getAvailableSensors())
                .setSlaveTypes (propMan.slaveTypes())
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
    //TODO: Можно (нужно?) после стирки выдавать статус CMD_NEED_SERVICE (с сообщением?),
    // который сбрасывается из консоли или какой-нибудь кнопкой Продолжить.
}
