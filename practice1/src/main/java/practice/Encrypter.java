package practice;
import java.nio.ByteBuffer;

public class Encrypter {

    public Encrypter(){

    }

    public byte[] encrypt(Message message){
        try {
            byte[] raw_text_bytes = message.get_message_string().getBytes();
            byte[] encrypted_text_bytes = CipherService.encrypt(raw_text_bytes);

            int wlen = 4 + 4 + encrypted_text_bytes.length;

            ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 8 + 4 + 2 + wlen + 2);
        
            buffer.put((byte)0x13);
            buffer.put((byte) message.get_unique_identefier());
            buffer.putLong(message.get_message_number());
            buffer.putInt(wlen);

            byte[] header = new byte[14];
            System.arraycopy(buffer.array(), 0, header, 0, 14);
            buffer.putShort(Crc16.calculateCrc(header));

            buffer.putInt(message.get_command_id());
            buffer.putInt(message.get_user_id());
            buffer.put(encrypted_text_bytes);

            byte[] payload = new byte[wlen];
            System.arraycopy(buffer.array(), 16, payload, 0, wlen);
            buffer.putShort(Crc16.calculateCrc(payload));

            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Encyption error:", e);
        }
    }
}