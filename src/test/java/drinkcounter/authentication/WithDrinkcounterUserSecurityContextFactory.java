package drinkcounter.authentication;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithDrinkcounterUserSecurityContextFactory implements WithSecurityContextFactory<WithDrinkcounterUser> {

    @Override
    public SecurityContext createSecurityContext(WithDrinkcounterUser annotation) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        DrinkcounterUserDetails principal = new DrinkcounterUserDetails(annotation.email(), "",
                true, true, true, true, authorities, annotation.userId());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        return context;
    }
}
