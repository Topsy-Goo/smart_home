package ru.gb.smarthome.homekeeper.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import static java.lang.String.format;

@Entity
@Table (name = "friendly_names")
@Data
public class FriendlyName {
    @Id
    @Column (name="uuid", nullable=false)
    private String uuid;

    @Column (name="name", nullable=false)
    private String name;

    private FriendlyName (){}
    public FriendlyName (String uid, String nam) {
        uuid = uid;
        name = nam;
    }

    @Override public String toString () {    return format ("%s / %s", uuid, name);    }
}
