package io.sourceripple.identity.application;

import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.RoleRepository;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    static final int MIN_PASSWORD_CHARS = 10;
    static final int MAX_PASSWORD_BYTES = 72;

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final SourceRippleProperties properties;
    private final Clock clock;

    public BootstrapAdminInitializer(
            UserRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder,
            SourceRippleProperties properties,
            Clock clock) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRolesName(RoleName.ADMIN)) {
            return;
        }
        SourceRippleProperties.Admin admin = properties.admin();
        if (admin == null || !StringUtils.hasText(admin.email()) || !StringUtils.hasText(admin.password())) {
            log.warn("No ADMIN account exists and SOURCERIPPLE_ADMIN_EMAIL or SOURCERIPPLE_ADMIN_PASSWORD is not set;"
                    + " no admin was created.");
            return;
        }
        requireUsablePassword(admin.password());

        String email = EmailAddresses.normalize(admin.email());
        // Promoting an existing account would let anyone who registers the admin email first become ADMIN.
        if (users.existsByEmailIgnoreCase(email)) {
            log.warn("No ADMIN account exists, but the configured admin email already belongs to an account;"
                    + " no admin was created.");
            return;
        }
        User user = new User(email, passwordEncoder.encode(admin.password()), "Administrator", clock.instant());
        user.addRole(roles.findByName(RoleName.ADMIN).orElseThrow());
        users.save(user);
        log.info("Created the bootstrap ADMIN account.");
    }

    // BCrypt cannot hash more than 72 bytes, and a bad value should stop startup rather than the first login.
    private static void requireUsablePassword(String password) {
        boolean tooShort = password.length() < MIN_PASSWORD_CHARS;
        boolean tooLong = password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
        if (tooShort || tooLong) {
            throw new IllegalStateException("SOURCERIPPLE_ADMIN_PASSWORD must be at least " + MIN_PASSWORD_CHARS
                    + " characters and at most " + MAX_PASSWORD_BYTES + " bytes in UTF-8.");
        }
    }
}
