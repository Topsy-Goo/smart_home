package ru.gb.smarthome.common.smart;


/** Основной интерфейс для всех умных устройств. */
public interface ISmartDevice extends Runnable
{
    void mainArgs (String[] args);
}
