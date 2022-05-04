package ru.gb.smarthome.empty.complex;

import ru.gb.smarthome.empty.client.DeviceClientEmpty;

/** Комплексные УУ могут выполнять роль мастера, роль слэйва или обе роли одновременно.   */
abstract public class DevClientEmptyComplex extends DeviceClientEmpty
{
    //protected ExecutorService sensorTimers;

    public DevClientEmptyComplex (PropManagerComplex pmc) {
        super(pmc);
    }
/*    protected Sensor searchSensorInAbilities (UUID uuid)
    {
        if (uuid != null)
        for (Sensor s : abilities.getSensors()) {
            if (s.getUuid().equals(uuid))
                return s;
        }
        return null;
    }*/

/*    protected Sensor searchSensorInDeviceState (UUID uuid)
    {
        if (uuid != null) {
            ;
            for (Sensor s : ) {
                if (s.getUuid().equals(uuid))
                    return s;
            }
        }
        return null;
    }*/

/*    @Override protected void onCmdPair (Object data) throws Exception {
        //Это нужно переопределить
        super.onCmdPair (data); //< заглушка
        ;
    }*/
}
