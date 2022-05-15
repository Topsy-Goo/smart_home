package ru.gb.smarthome.homekeeper.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gb.smarthome.homekeeper.dtos.AuthRequest;
import ru.gb.smarthome.homekeeper.dtos.RegisterRequest;
import ru.gb.smarthome.homekeeper.dtos.StringDto;
import ru.gb.smarthome.homekeeper.entities.OurUser;
import ru.gb.smarthome.homekeeper.services.AuthoService;
import ru.gb.smarthome.homekeeper.utils.JwtokenUtil;

@RestController
@RequestMapping ("/v1/auth")    //http://localhost:15550/home/v1/auth
@RequiredArgsConstructor
public class AuthController {

    private final AuthoService          authService;
    private final AuthenticationManager authenticationManager;
    private final JwtokenUtil           jwtokenUtil;

    //http://localhost:15550/home/v1/auth/login
    @PostMapping ("/login")
    public ResponseEntity<?> authenticateUser (@RequestBody AuthRequest authRequest)
    {
        String login = authRequest.getLogin();
        String password = authRequest.getPassword();
        return authentificateAndResponseWithJwt (login, password);
    }

    //http://localhost:15550/home/v1/auth/register
    @PostMapping ("/register")
    public ResponseEntity<?> registerNewUser (@RequestBody @Validated RegisterRequest registerRequest)
    {
        String login    = registerRequest.getLogin();
        String password = registerRequest.getPassword();
        String key      = registerRequest.getKey();
        OurUser ourUser = authService.validateKeyAndUpdateOurUser(login, password, key);

        if (ourUser != null)
            return authentificateAndResponseWithJwt (login, password);

        return new ResponseEntity<> (new StringDto ("Отказано в доступе."), HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<?> authentificateAndResponseWithJwt (String login, String password)
    {
        String errMsg;
        try {
            authenticationManager.authenticate (new UsernamePasswordAuthenticationToken (login, password));
        }
        catch (BadCredentialsException e) {
            errMsg = String.format ("\nНекорректные логин (%s)\rили пароль (%s).", login, password);
            return new ResponseEntity<> (new StringDto (errMsg), HttpStatus.UNAUTHORIZED);
        }
        catch (Exception e) {
            e.printStackTrace();
            errMsg = e.getClass().getSimpleName() +"; "+ e.getMessage();
            return new ResponseEntity<> (new StringDto (errMsg), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UserDetails userDetails = authService.loadUserByUsername (login);
        return ResponseEntity.ok (new StringDto (jwtokenUtil.generateJWToken (userDetails)));
    }
}
