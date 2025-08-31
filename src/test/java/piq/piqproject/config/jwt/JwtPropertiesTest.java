package piq.piqproject.config.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JwtPropertiesTest {
    @Test
    void testGetExpiration() {

    }

    @Test
    void testGetIssuer() {

    }

    @Test
    void testGetSecretKey() {
        JwtProperties jwtProperties = new JwtProperties();
        String secretKey = "privateKey";
        jwtProperties.setSecretKey(secretKey);
        assertEquals(secretKey, jwtProperties.getSecretKey());
    }

    @Test
    void testSetExpiration() {

    }

    @Test
    void testSetIssuer() {

    }

    @Test
    void testSetSecretKey() {

    }
}
