package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

@Data
public class StringDto {
    public String s;

    public StringDto (){}
    public StringDto (String s) { this.s = s; }
}
