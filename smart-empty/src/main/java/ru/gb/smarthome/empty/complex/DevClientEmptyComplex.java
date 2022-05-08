package ru.gb.smarthome.empty.complex;

import ru.gb.smarthome.empty.client.DeviceClientEmpty;

/** Комплексные УУ могут выполнять роль мастера, роль слэйва или обе роли одновременно.   */
abstract public class DevClientEmptyComplex extends DeviceClientEmpty
{
    public DevClientEmptyComplex (PropManagerComplex pmc) {
        super(pmc);
    }
}
