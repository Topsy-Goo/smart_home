package ru.gb.smarthome.homekeeper.entities;

import lombok.Data;

import javax.persistence.*;

import static java.lang.String.format;

@Entity
@Table (name="contracts")
@Data
public class Contract {
    @Id
    @Column (name="id")
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column (name="master_uuid", nullable=false)
    private String masterUuid;

    @Column (name="master_task", nullable=false)
    private String taskName;

    @Column (name="slave_uuid")
    private String slaveUuid;

    @Column (name="function_uuid", nullable=false)
    private String functionUuid;

    private Contract (){}
    public Contract (String mUuid, String tskName, String sUuid, String fUuid){
        masterUuid   = mUuid;
        taskName     = tskName;
        slaveUuid    = sUuid;
        functionUuid = fUuid;
    }

    @Override public String toString () {
        return format ("Contract:[%s (%s) | %s (%s)]"
                      ,masterUuid
                      ,taskName
                      ,slaveUuid
                      ,functionUuid);
    }
}
