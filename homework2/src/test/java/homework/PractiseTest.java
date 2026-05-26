package homework;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy; // Новий зручний імпорт

public class PractiseTest {

    private final Encrypter encrypter = new Encrypter();
    private final Decrypter decrypter = new Decrypter();

    @Test
    public void shouldEncryptMessage() {
        String original_json = "{\"secret_key\":\"top_secret\",\"should\"encrypt}";
        Message message = new Message((byte) 0x12, 43, 5, 78, original_json);

        byte[] encrypted_packet = encrypter.encrypt(message);
        String hex_result = Hex.encodeHexString(encrypted_packet);

        String hex_clear_text = Hex.encodeHexString(original_json.getBytes());
        assertThat(hex_result).doesNotContain(hex_clear_text);
    }

    @Test
    public void shouldEncrypAndDecrypt() {
        String original_json = "{\"secret_key\":\"top_secret\",\"should\"encrypt}";
        Message original_message = new Message((byte) 0x12, 43, 5, 78, original_json);

        byte[] packet_bytes = encrypter.encrypt(original_message);
        Message decrypted_message = decrypter.decrypt(packet_bytes);

        assertThat(decrypted_message.get_unique_identefier()).isEqualTo((byte) 0x12);
        assertThat(decrypted_message.get_message_number()).isEqualTo(43L);
        assertThat(decrypted_message.get_command_id()).isEqualTo(5);
        assertThat(decrypted_message.get_user_id()).isEqualTo(78);
        assertThat(decrypted_message.get_message_string()).isEqualTo(original_json);
    }

    @Test
    public void shouldThrowExceptionWhenPayloadIsCorrupted() {
        String original_json = "{\"secret_key\":\"top_secret\",\"should\"encrypt}";
        Message message = new Message((byte) 0x12, 43, 5, 78, original_json);

        byte[] valid_packet_bytes = encrypter.encrypt(message);
        valid_packet_bytes[16] = (byte) (valid_packet_bytes[16] ^ 0xFF);

        assertThatThrownBy(() -> decrypter.decrypt(valid_packet_bytes))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption error");
    }

    @Test
    public void shouldThrowExceptionWhenHeaderIsCorrupted() {
        String original_json = "{\"secret_key\":\"top_secret\",\"should\"encrypt}";
        Message message = new Message((byte) 0x12, 43, 5, 78, original_json);

        byte[] valid_packet_bytes = encrypter.encrypt(message);

        valid_packet_bytes[1] = (byte) 0x00;

        assertThatThrownBy(() -> decrypter.decrypt(valid_packet_bytes))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption error");
    }

    @Test
    public void shouldEncryptAndDecryptEmptyMessageString() {
        String empty_json = "";
        Message message = new Message((byte) 0x01, 1L, 0, 0, empty_json);

        byte[] packet_bytes = encrypter.encrypt(message);
        assertThat(packet_bytes).isNotEmpty();

        Message actual = decrypter.decrypt(packet_bytes);

        assertThat(actual.get_unique_identefier()).isEqualTo((byte) 0x01);
        assertThat(actual.get_message_number()).isEqualTo(1L);
        assertThat(actual.get_message_string()).isEqualTo(empty_json);
    }

    @Test
    public void shouldThrowExceptionWhenMagicByteIsInvalid() {
        String original_json = "{\"secret_key\":\"top_secret\",\"should\"encrypt}";
        Message message = new Message((byte) 0x12, 43, 5, 78, original_json);

        byte[] valid_packet_bytes = encrypter.encrypt(message);

        valid_packet_bytes[0] = (byte) 0x99;

        assertThatThrownBy(() -> decrypter.decrypt(valid_packet_bytes))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption error");
    }
}