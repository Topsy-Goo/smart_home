package ru.gb.smarthome.homekeeper.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gb.smarthome.homekeeper.services.AuthenticationService;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@RestController
@RequestMapping ("/v1/auth")    //http://localhost:15550/home/v1/auth
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;

}
