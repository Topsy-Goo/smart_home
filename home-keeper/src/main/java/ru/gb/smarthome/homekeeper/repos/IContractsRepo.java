package ru.gb.smarthome.homekeeper.repos;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.gb.smarthome.homekeeper.entities.Contract;

import java.util.List;

@Repository
public interface IContractsRepo extends CrudRepository<Contract, Integer>
{
    List<Contract> findAllByMasterUuid (String masterUuid);

    List<Contract> findAllByMasterUuidAndTaskNameAndSlaveUuidAndFunctionUuid (
                        String masterUuid,
                        String taskName,
                        String slaveUuid,
                        String functionUuid);

    List<Contract> findAllByFunctionUuid (String functionUuid);

/*    @Query (nativeQuery = true,
            value = "SELECT * FROM contracts WHERE "+
                        "master_uuid   = :m_uuid AND "+
                        "master_task   = :t_name AND "+
                        "slave_uuid    = :s_uuid AND "+
                        "function_uuid = :f_uuid AND ;")
    List<Contract> findContracts (
                            @Param("m_uuid")
                            @Param("t_name")
                            @Param("s_uuid")
                            @Param("f_uuid") );*/
}
