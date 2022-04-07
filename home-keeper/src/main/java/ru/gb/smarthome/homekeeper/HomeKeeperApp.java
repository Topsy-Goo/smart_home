package ru.gb.smarthome.homekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.homekeeper")
public class HomeKeeperApp {

    public static void main (String[] args) {
        SpringApplication.run (HomeKeeperApp.class, args);
    }

}
