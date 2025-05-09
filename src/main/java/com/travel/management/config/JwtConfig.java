package com.travel.management.config;

import com.travel.management.Utils.AuthTokenFilter;
import com.travel.management.Utils.JwtUtils;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.security.Key;

@Configuration
public class JwtConfig {

    @Bean
    public Key jwtSigningKey() {
        return Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    @Bean
    public AuthTokenFilter authTokenFilter(JwtUtils jwtUtils,
                                           @Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService) {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }
}
