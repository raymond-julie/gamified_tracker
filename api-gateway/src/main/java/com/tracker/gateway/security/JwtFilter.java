package com.tracker.gateway.security;

import com.tracker.gateway.auth.JwtUtil;
import com.tracker.gateway.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "userId";

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/auth")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
         HttpServletRequestWrapper requestWrapper ;
        try {
            var claims = jwtUtil.validateToken(token);
            String email = claims.getSubject();
            String roleClaim = claims.get("role", String.class);
            Role role = roleClaim != null ? Role.valueOf(roleClaim) : Role.USER;

            // IDOR fix: userId claim is required — an old token minted before this claim
            // existed must not silently proceed with no trusted identity downstream.
            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            var authorities = List.of(new SimpleGrantedAuthority(role.authority()));

            // IDOR fix: the previous wrapper only overrode getHeader(String), but the
            // Gateway's request forwarding enumerates headers via getHeaderNames()/
            // getHeaders(String) to build the downstream request — getHeader() alone is
            // never consulted there. That meant a client-forged "userId" header passed
            // straight through unmodified, and a request with no "userId" header at all
            // never got one added. Overriding all three closes both holes: the header is
            // always present exactly once, and it always carries the trusted value.
            // requestWrapper = new HttpServletRequestWrapper(request) {
            //     @Override
            //     public String getHeader(String name) {
            //         if ("userId".equals(name)) {
            //             return userId != null ? userId.toString() : null;
            //         }
            //         return super.getHeader(name);
            //     }
            // };
            final String trustedUserId = userId.toString();
            requestWrapper = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if (USER_ID_HEADER.equalsIgnoreCase(name)) {
                        return trustedUserId;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (USER_ID_HEADER.equalsIgnoreCase(name)) {
                        return Collections.enumeration(List.of(trustedUserId));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    names.removeIf(n -> USER_ID_HEADER.equalsIgnoreCase(n));
                    names.add(USER_ID_HEADER);
                    return Collections.enumeration(names);
                }
            };
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(requestWrapper, response);
    }
}
