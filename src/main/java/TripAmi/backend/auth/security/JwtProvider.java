package TripAmi.backend.auth.security;

import TripAmi.backend.app.util.Responder;
import TripAmi.backend.auth.authmember.domain.Role;
import TripAmi.backend.auth.exception.ExceptionCode;
import TripAmi.backend.auth.jwt.service.dto.TokenDto;
import TripAmi.backend.auth.security.infra.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.io.IOException;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Slf4j
@Component
public class JwtProvider {
    public static final String BEARER_TYPE = "Bearer";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REFRESH_HEADER = "Refresh";
    public static final String BEARER_PREFIX = "Bearer ";

    @Getter
    @Value("${jwt.secret-key}")
    private String secretKey;

    @Getter
    @Value("${jwt.access-token-expiration-millis}")
    private long accessTokenExpirationMillis;

    @Getter
    @Value("${jwt.refresh-token-expiration-millis}")
    private long refreshTokenExpirationMillis;
    private Key key;

    // Bean 등록후 Key SecretKey HS256 decode
    @PostConstruct
    public void init() {
        String base64EncodedSecretKey = encodeBase64SecretKey(this.secretKey);
        this.key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);
    }

    public String encodeBase64SecretKey(String secretKey) {
        return Encoders.BASE64.encode(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private Key getKeyFromBase64EncodedKey(String base64EncodedSecretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(base64EncodedSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenDto generateTokenDto(CustomUserDetails customUserDetails) {
        Date accessTokenExpiresIn = getTokenExpiration(accessTokenExpirationMillis);
        Date refreshTokenExpiresIn = getTokenExpiration(refreshTokenExpirationMillis);
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", customUserDetails.getRoles());

        String accessToken = Jwts.builder()
                                 .setClaims(claims)
                                 .setSubject(customUserDetails.getEmail())
                                 .setExpiration(accessTokenExpiresIn)
                                 .setIssuedAt(Calendar.getInstance().getTime())
                                 .signWith(key, SignatureAlgorithm.HS256)
                                 .compact();

        String refreshToken = Jwts.builder()
                                  .setSubject(customUserDetails.getEmail())
                                  .setIssuedAt(Calendar.getInstance().getTime())
                                  .setExpiration(refreshTokenExpiresIn)
                                  .signWith(key)
                                  .compact();

        return TokenDto.builder()
                   .grantType(BEARER_TYPE)
                   .authorizationType(AUTHORIZATION_HEADER)
                   .accessToken(accessToken)
                   .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                   .refreshToken(refreshToken)
                   .build();
    }

    // JWT 토큰을 복호화하여 토큰 정보를 반환
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("roles") == null) {
            //todo 에러 생성
            throw new RuntimeException("NO_ACCESS_TOKEN");
        }

        String[] authorities = claims.get("roles").toString().split(",");
        Set<Role> authoritySet = new HashSet<>();
        for (String authority : authorities) {
            String cleanedAuthority = authority.replaceAll("[\\[\\]]", "").toUpperCase();

            authoritySet.add(Role.valueOf(cleanedAuthority));
        }

        CustomUserDetails customUserDetails = CustomUserDetails.of(
            claims.getSubject(),
            authoritySet);

        log.info("# AuthMember.getRoles 권한 체크 = {}", customUserDetails.getAuthorities().toString());

        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }

    // 토큰 검증
    public boolean validateToken(String token, HttpServletResponse response) throws IOException {
        try {
            parseClaims(token);
        } catch (MalformedJwtException e) {
            log.info("Invalid JWT");
            log.trace("Invalid JWT trace = {}", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT");
            log.trace("Expired JWT trace = {}", e);
            Responder.sendErrorResponse(response, ExceptionCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT");
            log.trace("Unsupported JWT trace = {}", e);
            Responder.sendErrorResponse(response, ExceptionCode.TOKEN_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.");
            log.trace("JWT claims string is empty trace = {}", e);
            Responder.sendErrorResponse(response, ExceptionCode.TOKEN_ILLEGAL_ARGUMENT);
        }
        return true;
    }

    private Date getTokenExpiration(long expirationMillisecond) {
        Date date = new Date();

        return new Date(date.getTime() + expirationMillisecond);
    }

    // Token 복호화 및 예외 발생(토큰 만료, 시그니처 오류)시 Claims 객체가 안만들어짐.
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(key)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

    public void accessTokenSetHeader(String accessToken, HttpServletResponse response) {
        String headerValue = BEARER_PREFIX + accessToken;
        response.setHeader(AUTHORIZATION_HEADER, headerValue);
    }

    public void refresshTokenSetHeader(String refreshToken, HttpServletResponse response) {
        response.setHeader("Refresh", refreshToken);
    }

    // Request Header에 Access Token 정보를 추출하는 메서드
    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // Request Header에 Refresh Token 정보를 추출하는 메서드
    public String resolveRefreshToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(REFRESH_HEADER);
        if (StringUtils.hasText(bearerToken)) {
            return bearerToken;
        }
        return null;
    }
}
