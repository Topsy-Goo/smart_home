package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.ISmartHandler;

public interface IDeviceServer extends Runnable {

/** Вызывается хэндлером, когда он завершает работу. */
    void goodBy (ISmartHandler device);
}
