package ru.gb.smarthome.sequrity.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication (scanBasePackages = "ru.gb.smarthome.sequrity.controller")
public class SequrityControllerApp {

    public static final boolean DEBUG = true;

	public static void main(String[] args) {
		SpringApplication.run (SequrityControllerApp.class, args);
	}

}
