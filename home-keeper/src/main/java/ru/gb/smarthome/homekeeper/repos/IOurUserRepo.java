package ru.gb.smarthome.homekeeper.repos;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.gb.smarthome.homekeeper.entities.OurUser;

import java.util.Optional;

@Repository
public interface IOurUserRepo extends CrudRepository<OurUser, Long>
{
    Optional<OurUser> findByLogin (String login);
}
