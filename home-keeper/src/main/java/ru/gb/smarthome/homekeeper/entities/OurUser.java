package ru.gb.smarthome.homekeeper.entities;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table (name = "users")
@Data
public class OurUser
{
    @Id
    @Column (name="id")
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="login", nullable=false, unique=true)
    private String login;

    @Column(name="password", nullable=false)
    private String password;

    @UpdateTimestamp
    @Column(name="updated_at")    @Getter
    @Setter
    private LocalDateTime updatedAt;

    public OurUser (){}
    public OurUser (@NotNull String log, @NotNull String pass) {
        login = log;
        password = pass;
    }
}
