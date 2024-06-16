package com.example.server_android.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET_KEY = "4436646A707A654579493565306A3476736D696A686544787844326C747A7A385844796A7770764E4D4D5369524677377A4746675248375438716C5262706437396673696E37334F6868366A4D703779425270554773387065544152666D6D7342726E72345438505A3650566B4864783051674D644B6E6670675A416A697679394D3565644D51504F74515A614F6149547942596E766B357750577747597836763974624C766A4A62427378566C4131376267325830743472497A516A6D783676617A67303138324175715A4D434839336F39574D4A64634F6D426F6245374A34317741767168534E724445434E755867386A3743734D356C356D355049754C4E6134547A6D6C454B707776324737556350304E564E776743414B3245554732446C4D324259556C776F4B58427A46664C6E736D5232674E636D64644E64385A55366E424769514762646A756450625449704E3565413736674B686C5670416A54384F5A364D59434B56455151774A30657966434A765251706B4474636248314E6B33587A5A47535772327A5844364B556556646A417751644466624F593730457139306336576E6C746368557252374D6371763159424F4C307830685A69746E5056346D385559646A58456854744377704846595A737A566234755330376A535354695635434E3834495051783839666C5251717A7959616A7A6A4C353032";
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token, Claims::getSubject);
    }

    public <T> T getClaimsFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return doGenerateToken(new HashMap<>(), userDetails);
    }

    private String doGenerateToken(Map<String, Object> extraClaims,UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
                .signWith(getSignKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    private Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token, Claims::getExpiration);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
