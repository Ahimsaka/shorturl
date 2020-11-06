package com.github.ahimsaka.shorturl.shorturl.webconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {
    Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);
    DataSource dataSource;
    PasswordEncoder passwordEncoder;

    public WebSecurityConfig(DataSource dataSource){
        this.dataSource = dataSource;
        this.passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {
        return http.authorizeExchange()
                .pathMatchers("/user")
                .authenticated()
                .anyExchange().permitAll()
                .and()
                .oauth2Login()
                .and()
                .formLogin()
                .and()
                .build();
    }
/*
    @Bean
    public UserDetailsService userDetailsService(String username) {
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
        log.info(username);
        return new UserDetailsService(users.loadUserByUsername(username));
    }


    public interface ReactiveUserDetailsService {
        Mono<UserDetails> findByUsername(String username);
    }


    @Bean
    UserDetailsManager users(DataSource dataSource) {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();
        UserDetails admin = User.builder()
                .username("admin")
                .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
                .roles("USER", "ADMIN")
                .build();
        users.createUser(user);
        users.createUser(admin);

        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
        return users;
    }
    */
}

