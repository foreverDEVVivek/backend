package devPilot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class EncryptionConfig {

    @Bean
    public TextEncryptor textEncryptor() {
        return new TextEncryptor() {

            private final SecureRandom random = new SecureRandom();
            @Value("${spring.token.encryption.password}")
            String password;

            // System.out.println ("Password for encryption: " + password);
            private SecretKeySpec deriveKey(byte[] salt)
                    throws Exception {
                PBEKeySpec spec = new PBEKeySpec(
                        password.toCharArray(),
                        salt,
                        210_000,
                        256
                );

                try {
                    byte[] key = SecretKeyFactory
                            .getInstance("PBKDF2WithHmacSHA256")
                            .generateSecret(spec)
                            .getEncoded();

                    return new SecretKeySpec(key, "AES");
                } finally {
                    spec.clearPassword();
                }
            }

            @Override
            public String encrypt(String text) {
                try {
                    byte[] salt = new byte[16];
                    byte[] iv = new byte[12];
                    random.nextBytes(salt);
                    random.nextBytes(iv);

                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(
                            Cipher.ENCRYPT_MODE,
                            deriveKey(salt),
                            new GCMParameterSpec(128, iv)
                    );

                    byte[] encrypted = cipher.doFinal(
                            text.getBytes(StandardCharsets.UTF_8)
                    );

                    ByteBuffer buffer = ByteBuffer.allocate(
                            salt.length + iv.length + encrypted.length
                    );

                    buffer.put(salt).put(iv).put(encrypted);

                    return Base64.getEncoder()
                            .encodeToString(buffer.array());

                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Token encryption failed", e
                    );
                }
            }

            @Override
            public String decrypt(String encryptedText) {
                try {
                    byte[] data = Base64.getDecoder()
                            .decode(encryptedText);

                    ByteBuffer buffer = ByteBuffer.wrap(data);

                    byte[] salt = new byte[16];
                    byte[] iv = new byte[12];

                    buffer.get(salt);
                    buffer.get(iv);

                    byte[] ciphertext = new byte[buffer.remaining()];
                    buffer.get(ciphertext);

                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(
                            Cipher.DECRYPT_MODE,
                            deriveKey(salt),
                            new GCMParameterSpec(128, iv)
                    );

                    return new String(
                            cipher.doFinal(ciphertext),
                            StandardCharsets.UTF_8
                    );

                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Token decryption failed", e
                    );
                }
            }
        };
    }
}
