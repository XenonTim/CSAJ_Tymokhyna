package practice;
import java.nio.ByteBuffer;

public class Decrypter {
    public Decrypter(){

    }

    public Message decrypt(byte[] message_to_decrypt) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message_to_decrypt);
            byte magic_byte = buffer.get();
            if (magic_byte != (byte) 0x13) {
                throw new IllegalArgumentException("Invalid magic byte");
            }

            byte unique_identifier_byte = buffer.get();
            long message_number = buffer.getLong();
            int wlen = buffer.getInt();
            short first_crc = buffer.getShort();

            short checksum = Crc16.calculateCrc(message_to_decrypt, 0, 14);
            validate_checksum(checksum, first_crc);

            int command_id = buffer.getInt();
            int user_id = buffer.getInt();

            byte[] encrypted_payload = new byte[wlen - 8];
            buffer.get(encrypted_payload);
        
            short second_crc = buffer.getShort();
            short checksum2 = Crc16.calculateCrc(message_to_decrypt, 16, wlen);
            validate_checksum(checksum2, second_crc);

            byte[] decrypted_text_bytes = CipherService.decrypt(encrypted_payload);
            String decrypted_string = new String(decrypted_text_bytes);

            return new Message(unique_identifier_byte, message_number, command_id, user_id, decrypted_string);
            } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }

    private void validate_checksum(short expected_sum, short actual_sum){
        if (actual_sum != expected_sum){
            throw new IllegalArgumentException("Checksum does not match");
        }
    }
}
