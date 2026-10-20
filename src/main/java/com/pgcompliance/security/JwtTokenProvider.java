package com.pgcompliance.security;

import com.pgcompliance.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

  @Value("${app.jwt.secret}")
  private String secretKey;;

  private static final long TOKEN_VALIDITY = 24 * 60 * 60 * 1000L;

  private Key getSigningKey() {

    return Keys.hmacShaKeyFor(
        secretKey.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(User user) {

    Date now = new Date();

    Date expiryDate = new Date(now.getTime() + TOKEN_VALIDITY);

    return Jwts.builder()
        .setSubject(user.getUsername())
        .claim("role", user.getRole().name())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public String getUsernameFromToken(
      String token) {

    return getClaims(token).getSubject();
  }

  public String getRoleFromToken(
      String token) {

    return getClaims(token).get(
        "role",
        String.class);
  }

  public boolean validateToken(
      String token) {

    try {

      Claims claims = getClaims(token);

      return claims.getExpiration()
          .after(new Date());

    } catch (Exception exception) {

      return false;
    }
  }

  private Claims getClaims(
      String token) {

    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}