package ru.gb.smarthome.homekeeper;

@FunctionalInterface
public interface IAddNewsCallback {
    void callback (String... news);
}
