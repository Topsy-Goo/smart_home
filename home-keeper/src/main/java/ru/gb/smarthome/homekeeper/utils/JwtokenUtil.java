package ru.gb.smarthome.homekeeper.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.gb.smarthome.common.FactoryCommon.longFromLdt;

@Component
public class JwtokenUtil
{
    @Value ("${jwt.secret}")    private String  secret;
    @Value ("${jwt.lifetime}")  private Integer lifetime;


    public String generateJWToken (UserDetails userDetails)
    {
        Map<String, Object> claims = new HashMap<>();
        List<String> roles = userDetails
                                .getAuthorities()
                                .stream()
                                .map (GrantedAuthority::getAuthority)
                                .collect (Collectors.toList());

    //Помещаем в JWT список ролей (в качестве одного из элементов claims), логин юзера,
    // дату создания токена и срок годности токена (в качестве трёх других элементов claims).
    // «Закрепляем» данные указанием секрета и типа шифрования.

        claims.put ("roles", roles);
        Date dateIssued  = new Date();
        Date dateExpired = new Date (dateIssued.getTime() + lifetime);
        //LocalDateTime ldtNow = LocalDateTime.now();
        //Date dateIssued  = new Date (longFromLdt (ldtNow));
        //LocalDateTime ldtNowPlusCentury = ldtNow.plusYears (100);
        //Date dateExpired = new Date (longFromLdt (ldtNowPlusCentury));

        String s = Jwts.builder()
                   .setClaims (claims)
                   .setSubject (userDetails.getUsername())
                   .setIssuedAt (dateIssued)
                   .setExpiration (dateExpired)
                   .signWith (SignatureAlgorithm.HS256, secret)
                   .compact();
        return s;
    }

    public String getLoginFromToken (String token) {
        return getClaimFromToken (token, Claims::getSubject);
    }

    private <T> T getClaimFromToken (String token, Function<Claims, T> claimsResolver)
    {
        Claims claims = getAllClaimsFromToken (token);
        return claimsResolver.apply (claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                   .setSigningKey (secret)  //< нужет для проверки подлинности и актуальности токена
                   .parseClaimsJws (token)
                   .getBody();
    }
}
