package ru.gb.smarthome.sequrity.camera;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.complex.PropManagerComplex;

import javax.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.AUTONOMIC;
import static ru.gb.smarthome.common.FactoryCommon.INTERRUPTIBLE;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SEQURITY_CONTROLLLER;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.WASHER;

@Component ("seqcam_propman")
@Scope ("singleton")
public class PropManagerSequrCamera extends PropManagerComplex
{
    private final String name  = "Hikvision DS-2CD2023G0E-I(B)";
    private final UUID   uuid  = UUID.fromString ("a1e44609-1af8-43bf-824e-eac069feeb92");

    @PostConstruct
    @Override public void init() {
        tasks.add (new Task ("регистратор",                AUTONOMIC, 0L, TimeUnit.SECONDS, INTERRUPTIBLE).setMessage("Начать запись."));
        tasks.add (new Task ("включение по датчику",       AUTONOMIC, 0L, TimeUnit.SECONDS, INTERRUPTIBLE).setMessage("Начать дежурство."));
        tasks.add (new Task ("режим просмотра (стриминг)", AUTONOMIC, 0L, TimeUnit.SECONDS, INTERRUPTIBLE).setMessage("Начать передачу видео."));
        slaveTypes.add (SEQURITY_CONTROLLLER);
        slaveTypes.add (WASHER);
    }

    @Override public UUID getUuid ()                { return uuid; }
    @Override public String getName ()              { return name; }
}
