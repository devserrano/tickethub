package com.tickethub.security;

import com.tickethub.entity.User;
import com.tickethub.enums.Role;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * El usuario "tal como lo ve Spring Security". Es un envoltorio de la entidad User:
 * así la seguridad no depende de JPA y el resto de la app puede pedir el id o el rol
 * de quien hace la petición con @AuthenticationPrincipal.
 */
@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public AuthenticatedUser(Long id, String name, String email, String passwordHash, Role role, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getName(), user.getEmail(),
                user.getPasswordHash(), user.getRole(), user.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // el prefijo ROLE_ es la convención de Spring: hasRole("ADMIN") busca ROLE_ADMIN
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Un usuario desactivado no puede entrar, ni con un token que todavía no vence. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
