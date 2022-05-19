package ru.gb.smarthome.sequrity.camera;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.AUTONOMIC;
import static ru.gb.smarthome.common.FactoryCommon.INTERRUPTIBLE;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SEQURITY_CONTROLLLER;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.WASHER;
import static ru.gb.smarthome.sequrity.camera.FactorySequrCamera.TASKNAME_STREAMING;

@Component ("seqcam_propman")
@Scope ("singleton")
public class PropManagerSequrCamera extends PropertyManagerEmpty
{
    private final String name  = "Hikvision DS-2CD2023G0E-I(B)";
    private final UUID   uuid  = UUID.fromString ("a1e44609-1af8-43bf-824e-eac069feeb92");

    @PostConstruct
    @Override public void init() {
        tasks.add (new Task (TASKNAME_STREAMING, AUTONOMIC, INTERRUPTIBLE, 1L, TimeUnit.HOURS).setMessage("Начать трансляцию."));
//      tasks.add (new Task (TASKNAME_VRECORDER, AUTONOMIC, INTERRUPTIBLE, 5L, TimeUnit.MINUTES).setMessage("Начать запись."));
        slaveTypes.add (SEQURITY_CONTROLLLER);
        slaveTypes.add (WASHER);
    }

    @Override public UUID getUuid ()                { return uuid; }
    @Override public String getName ()              { return name; }
}
