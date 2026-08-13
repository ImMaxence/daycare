package com.back.daycare.config;

import com.back.daycare.entity.User;
import com.back.daycare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-user.username:admin}")
    private String defaultUsername;

    @Value("${app.default-user.password:admin123}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername(defaultUsername)) {
            log.info("Utilisateur par défaut '{}' déjà présent, aucune action.", defaultUsername);
            return;
        }

        User user = User.builder()
                .username(defaultUsername)
                .password(passwordEncoder.encode(defaultPassword))
                .build();

        userRepository.save(user);
        log.info("Utilisateur par défaut '{}' créé avec succès.", defaultUsername);
    }
}

