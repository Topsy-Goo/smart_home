package ru.gb.smarthome.homekeeper;

import ru.gb.smarthome.common.smart.structures.Signal;

import java.util.UUID;

@FunctionalInterface
public interface ISignalCallback {
    void callback (UUID masterUuid, Signal signal);
}
