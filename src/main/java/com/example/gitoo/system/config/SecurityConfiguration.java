package com.example.gitoo.system.config;

import com.example.gitoo.system.security.JwtTokenFilter;
import com.example.gitoo.system.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    public final JwtTokenProvider jwtTokenProvider;
    public final AuthenticationProvider authenticationProvider;
    private final JwtTokenFilter jwtTokenFilter;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf->csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // ✅ 추가
                        .requestMatchers("/", "/index.html",
                                "/login", "/login.html",
                                "/signup", "/signup.html",
                                "/main", "/main.html",
                                "/actuator/**",
                                "/game/index.html",
                                "/game/index.html","/game/**",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/schools/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/ws-wordchain/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/rooms/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/game/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/game/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://localhost:63342" // ✅ 추가 (지금 프론트 오리진)
                // 필요하면 "http://127.0.0.1:63342" 도 추가
        ));

        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS")); // ✅ OPTIONS 추가
        configuration.setAllowedHeaders(List.of("*")); // ✅ 편하게 (개발 중)
        configuration.setAllowCredentials(true); // ✅ 쿠키/세션 쓰면 필요 (지금은 켜도 무방)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}
