package io.sourceripple.identity.infrastructure.security;

import io.sourceripple.identity.application.EmailAddresses;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser loadUserByUsername(String email) {
        User user = users.findByEmailIgnoreCase(EmailAddresses.normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("No account for this email."));
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.isEnabled(), authorities);
    }
}
