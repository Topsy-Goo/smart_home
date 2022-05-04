package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import static java.lang.String.format;

@Data
public class UuidDto {
    String displayName;
    String uuid;

    public UuidDto (){}
    public UuidDto (String name, String uu)   { uuid = uu;    displayName = name; }

    @Override public String toString () {
        return format("[%s, %s]", displayName, uuid);
    }
}
