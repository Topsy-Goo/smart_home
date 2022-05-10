package ru.gb.smarthome.homekeeper.repos;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.gb.smarthome.homekeeper.entities.FriendlyName;

@Repository
public interface IFriendlyNamesRepo extends CrudRepository<FriendlyName, String>
{
    FriendlyName findByUuid (String uuid);
}
