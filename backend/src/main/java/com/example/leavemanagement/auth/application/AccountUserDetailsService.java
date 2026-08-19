package com.example.leavemanagement.auth.application;

import com.example.leavemanagement.auth.persistence.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final UserAccountRepository accounts;
    public AccountUserDetailsService(UserAccountRepository accounts){this.accounts=accounts;}
    @Override public UserDetails loadUserByUsername(String username){var account=accounts.findByNormalizedLogin(username.strip().toLowerCase(Locale.ROOT)).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));var authorities=account.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+r)).toList();return User.withUsername(account.getId().toString()).password(account.getPasswordHash()).disabled(!account.isEnabled()).authorities(authorities).build();}
}

