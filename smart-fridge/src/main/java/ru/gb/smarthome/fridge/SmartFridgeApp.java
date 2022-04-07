package ru.gb.smarthome.fridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.fridge")
public class SmartFridgeApp {

    public static void main (String[] args) {
        SpringApplication.run (SmartFridgeApp.class, args);
    }

}
