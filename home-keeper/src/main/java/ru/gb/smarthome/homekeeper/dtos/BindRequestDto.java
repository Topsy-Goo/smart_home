package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import static java.lang.String.format;

@Data
public class BindRequestDto {
    private String masterTaskName;
    private String masterUUID;
    private String slaveUUID;
    private String slaveFuctionUUID;

    public BindRequestDto(){}

    @Override public String toString () {
        return format ("[MASTER: %s / %s,\n SLAVE: %s, slave FUNCTION: %s]",
                       masterTaskName, masterUUID, slaveUUID, slaveFuctionUUID);
    }
}
