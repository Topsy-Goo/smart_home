package ru.gb.smarthome.homekeeper.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.gb.smarthome.homekeeper.services.AuthoService;

@EnableWebSecurity  //< «включатель» правил безопасности, описанных в этом классе
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
    private final AuthoService     authService;
    private final JwtRequestFilter jwtRequestFilter;

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean () throws Exception //< TODO: удалить?
    {
        return super.authenticationManagerBean();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder()  {   return new BCryptPasswordEncoder();   }

    @Override
    protected void configure (HttpSecurity httpSec) throws Exception
    {
        httpSec.csrf().disable()
               .authorizeRequests()
               .antMatchers ("/v1/main/**").authenticated()
               .antMatchers ("/v1/schedule/**").authenticated()
               .and()
               .sessionManagement().sessionCreationPolicy (SessionCreationPolicy.STATELESS)
               .and()
               .headers().frameOptions().disable()
               .and()
               .exceptionHandling()
               .authenticationEntryPoint (new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
               ;
        httpSec.addFilterBefore (jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
