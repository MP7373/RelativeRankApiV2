package com.relativerank.api.security;

import com.relativerank.api.util.Constants;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Component
@Profile("!test")
public class HmacSha512PasswordEncoder {

    public byte[] generateRandomSalt() {
        var random = new SecureRandom();
        var salt = new byte[90];
        random.nextBytes(salt);

        return salt;
    }

    public byte[] hmacSha512HashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeyException {
        var sha512Hmac = Mac.getInstance(Constants.HMAC_SHA_512);
        var secret = new SecretKeySpec(salt, Constants.HMAC_SHA_512);
        sha512Hmac.init(secret);
        return sha512Hmac.doFinal(Utf8.encode(password));
    }
}
