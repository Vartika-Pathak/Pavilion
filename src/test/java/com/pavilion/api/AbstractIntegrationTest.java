package com.pavilion.api;

import com.pavilion.api.entity.User;
import com.pavilion.api.repository.UserRepository;
import com.pavilion.api.security.JwtAuthenticationFilter;
import com.pavilion.api.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Full Spring context + real MockMvc, so requests go through the actual Spring Security filter
// chain (JwtAuthenticationFilter, SecurityConfig's authorization rules) exactly as they would in
// production — the same thing that was verified by hand with curl before this refactor shipped,
// now pinned down as a regression suite. @Transactional rolls each test's DB writes back
// afterwards so tests in the same class don't see each other's data.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected User createUser(String role) {
        User user = new User();
        user.setName("Test " + role);
        user.setEmail(role + "-" + System.nanoTime() + "@test.local");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFlatNumber("A1");
        user.setRole(role);
        return userRepository.save(user);
    }

    protected Cookie sessionCookie(User user) {
        return new Cookie(JwtAuthenticationFilter.SESSION_COOKIE, jwtService.signSessionToken(user.getId()));
    }
}
