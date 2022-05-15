package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

@Data
public class RegisterRequest
{
    private String login;
    private String password;
    private String key;

    public RegisterRequest () {}
}
