package zw.co.cresolzim.schoolms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import zw.co.cresolzim.schoolms.domain.Role;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginSuccessHandler successHandler)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/login", "/error").permitAll()

                // Administration
                .requestMatchers("/setup/**", "/users/**", "/academics/years/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                // Student records: admin and head write, teachers read
                .requestMatchers("/students/*/expel", "/students/*/transfer", "/students/*/discipline/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)
                .requestMatchers("/students/new", "/students/*/edit")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                .requestMatchers("/staff/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                .requestMatchers("/timetable/edit/**", "/timetable/slot/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                .requestMatchers("/attendance/staff/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                .requestMatchers("/library/catalogue/new", "/library/catalogue/*/edit",
                                 "/library/issue/**", "/library/return/**")
                    .hasAnyAuthority(Role.ADMIN, Role.LIBRARIAN)

                // Scheme of work: everyone reads, the head writes.
                .requestMatchers("/curriculum/new", "/curriculum/*/edit",
                                 "/curriculum/*/retire", "/curriculum/copy")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                // Lesson plan review is the head's; writing is the teacher's.
                .requestMatchers("/lessons/*/approve", "/lessons/*/return", "/lessons/*/reopen")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                // Class and subject allocation is set by the head, read by teachers.
                .requestMatchers("/academics/classes/new", "/academics/classes/*/edit",
                                 "/academics/classes/*/promote", "/academics/subjects/**",
                                 "/academics/classrooms/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD)

                .requestMatchers("/boarding/setup", "/boarding/hostels/**", "/boarding/dormitories/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD, Role.MATRON)

                .requestMatchers("/boarding/**")
                    .hasAnyAuthority(Role.ADMIN, Role.HEAD, Role.MATRON)

                // Guardian portal
                .requestMatchers("/portal/**").hasAuthority(Role.GUARDIAN)

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?signedout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/denied"));

        return http.build();
    }
}
