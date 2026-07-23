package com.pavilion.api.security;

import com.pavilion.api.entity.User;
import com.pavilion.api.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserResolver {

    public static final String SESSION_COOKIE = "session";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CurrentUserResolver(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public Optional<User> resolve(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return jwtService.verifySessionToken(cookie.getValue()).flatMap(userRepository::findById);
            }
        }
        return Optional.empty();
    }
}
