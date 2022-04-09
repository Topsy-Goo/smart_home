package ru.gb.smarthome.common.smart.structures;

import ru.gb.smarthome.common.smart.ISmartDevice;

import java.net.Socket;

import static java.lang.String.format;

/** Универсальный порт умного дома, к которому может быть подключено любое УУ.<p>
Port используется, чтобы мы могли эмулировать ограниченное количество подключаемых устройств.
Перед подключением устройства УД проверяет, есть ли в массиве свободные элементы — экземпляры Port с полем
device == null.  */
public class Port {
    private Socket socket;
    private ISmartDevice device;

    public Socket getSocket () { return socket; }
    public ISmartDevice getDevice () { return device; }
    public void setSocket (Socket val) { socket = val; }
    public void setDevice (ISmartDevice val) { device = val; }

/** Инициализируем поля экземпляра Port (с проверкой значений, т.к.
по полю ISmartDevice device приложение определяет, свободен ли экземпляр Port). */
    public boolean occupy (Socket s, ISmartDevice d)
    {
        boolean ok = d != null && s != null;
        if (ok) {
            device = d;
            socket = s;
        }
        return ok;
    }

/** Определяем, свободен ли экземпляр Port. Порт считается свободным, если его поле device == null. */
    public boolean isFree () { return getDevice() == null; }

/** Обнуляем поля экземпляра Port. */
    public void freePort () { socket = null;  device = null; }

    @Override public String toString () { return format ("Port[socket: %s, device: %s]", socket, device); }
}
