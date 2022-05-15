package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

@Data
public class AuthRequest {
    private String login;
    private String password;

    public AuthRequest (){}
}
