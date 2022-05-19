package ru.gb.smarthome.sequrity.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.util.UUID;

import static ru.gb.smarthome.common.FactoryCommon.BINDABLE;
import static ru.gb.smarthome.common.smart.enums.SensorStates.SST_OFF;
import static ru.gb.smarthome.common.smart.enums.SensorTypes.*;

//import static ru.gb.smarthome.common.FactoryCommon.USE_DEF_SENSOR_NAME;

@Component ("seqctrl_propman")
@Scope ("singleton")
public class PropManagerSequrController extends PropertyManagerEmpty
{
    private final String name = "Спайдер-М4";
    private final UUID   uuid = UUID.fromString ("f02d7de3-b738-4f16-bbed-3e5fe073f533");

    @PostConstruct
    @Override public void init() {
        sensors.add (new Sensor (SNT_OPENING, "Датчик открывания 1", SST_OFF, BINDABLE, UUID.fromString ("b413d6ab-6960-4289-bff0-8024fbac82da")));
        sensors.add (new Sensor (SNT_OPENING, "Датчик открывания 2", SST_OFF, BINDABLE, UUID.fromString ("b7c3bac4-2e86-41a9-bbf9-e9a25a5678bf")));
        sensors.add (new Sensor (SNT_MOVE,    "Датчик движения",     SST_OFF, BINDABLE, UUID.fromString ("bc1b11ef-ae32-48ec-9249-828832148fad")));
        sensors.add (new Sensor (SNT_FIRE,    "Датчик пожарный",     SST_OFF, BINDABLE, UUID.fromString ("9b3435d9-3c6c-49a3-8859-3ac95bf30aee")));
    }

    @Override public UUID getUuid ()   { return uuid; }
    @Override public String getName () { return name; }
}
