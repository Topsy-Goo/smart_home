package ru.gb.smarthome.empty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.empty")
public class EmptyApp {

    public static void main (String[] args) {
        SpringApplication.run (EmptyApp.class, args);
    }

}
