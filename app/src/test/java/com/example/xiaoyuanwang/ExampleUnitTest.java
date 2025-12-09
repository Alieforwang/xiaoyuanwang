package com.example.xiaoyuanwang;

import org.junit.Test;
import java.util.Base64;
import com.example.xiaoyuanwang.utils.XXTEACipher;
import com.example.xiaoyuanwang.api.SrunPortal;
import org.json.JSONObject;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void xxtea_encryption_isCorrect() {
        String data = "Hello World";
        String key = "1234567890123456";
        String expectedBase64 = "g3g+gyvNuKn3kgWqRwHj/Q==";

        byte[] encrypted = XXTEACipher.encrypt(data, key);
        // Unit tests run on host JVM (not Android), so we use java.util.Base64
        String actualBase64 = Base64.getEncoder().encodeToString(encrypted);

        assertEquals(expectedBase64, actualBase64);
    }
}
