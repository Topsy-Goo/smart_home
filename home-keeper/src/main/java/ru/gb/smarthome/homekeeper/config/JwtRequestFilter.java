package ru.gb.smarthome.homekeeper.config;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.gb.smarthome.homekeeper.services.AuthoService;
import ru.gb.smarthome.homekeeper.utils.JwtokenUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ru.gb.smarthome.common.FactoryCommon.lnerrprintln;
import static ru.gb.smarthome.homekeeper.FactoryHome.AUTHORIZATION_HDR_TITLE;
import static ru.gb.smarthome.homekeeper.FactoryHome.BEARER_;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter
{
    private final JwtokenUtil  jwtokenUtil;
    private final AuthoService authService;


    @Override
    protected void doFilterInternal (@NotNull HttpServletRequest  request,
                                     @NotNull HttpServletResponse response,
                                     @NotNull FilterChain         filterChain)
                             throws ServletException, IOException
    {
        String login = null;
        String jwt   = null;
        String authHeader = request.getHeader (AUTHORIZATION_HDR_TITLE);

        if (authHeader != null && authHeader.startsWith (BEARER_))
        {
            jwt = authHeader.substring (BEARER_.length());
            try {
                login = jwtokenUtil.getLoginFromToken (jwt);
            }
            catch (ExpiredJwtException e) {
                e.printStackTrace();
                lnerrprintln ("The token is expired");
            }
        }

        if (login != null && SecurityContextHolder.getContext().getAuthentication() == null)
        {
            UsernamePasswordAuthenticationToken token = trustDatabaseOnly (login, jwt, request);
            SecurityContextHolder.getContext().setAuthentication (token);
        }
        filterChain.doFilter (request, response);
    }

    private UsernamePasswordAuthenticationToken
                trustDatabaseOnly (String login, String jwt, HttpServletRequest request)
    {
        UserDetails userDetails = authService.loadUserByUsername (login);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken (
                    userDetails,
                    null,
                    userDetails.getAuthorities());

        token.setDetails (new WebAuthenticationDetailsSource().buildDetails (request));
        return token;
    }
}
