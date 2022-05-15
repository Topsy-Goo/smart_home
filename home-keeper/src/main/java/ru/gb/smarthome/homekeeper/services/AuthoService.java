package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gb.smarthome.homekeeper.PropertyManagerHome;
import ru.gb.smarthome.homekeeper.entities.OurUser;
import ru.gb.smarthome.homekeeper.repos.IOurUserRepo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthoService implements UserDetailsService
{
    private final IOurUserRepo        ourUserRepo;
    private final PropertyManagerHome propMan;

    @Override
    @Transactional
    public UserDetails loadUserByUsername (String login)
    {
        OurUser ourUser = findByLogin(login).orElse(null);
        return (ourUser == null)
                    ? null
                    : new User (ourUser.getLogin(),
                                ourUser.getPassword(),
                                mapRolesToAuthorities());
    }

    public Optional<OurUser> findByLogin (String login) {
        return ourUserRepo.findByLogin (login);
    }

    Collection<? extends GrantedAuthority> mapRolesToAuthorities () {
        return new LinkedList<>(Collections.singletonList (new SimpleGrantedAuthority ("ADMIN")));
    }

    public OurUser validateKeyAndUpdateOurUser (String login, String password, String key)
    {
        if (isKeyValid (key)) {
            OurUser ourUser = ourUserRepo.findByLogin (login).orElse(null);
            if (ourUser != null)
            {
                ourUser.setLogin (login);
                ourUser.setPassword (new BCryptPasswordEncoder().encode (password));
                return ourUserRepo.save (ourUser);
            }
        }
        return null;
    }

    private boolean isKeyValid (String key) {    return propMan.getKey().equals (key);    }
}
