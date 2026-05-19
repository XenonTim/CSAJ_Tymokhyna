package practice;

public class Message {
    private byte unique_identefier;
    private long message_number;
    private int command_id;
    private int user_id;
    private String message_string;

    public Message (byte unique_identefier, long message_number, int command_id, int user_id, String message_string) {
        this.unique_identefier = unique_identefier;
        this.message_number = message_number;
        this.command_id = command_id;
        this.user_id = user_id;
        this.message_string = message_string;
    }
    
    public long get_message_number() {
        return message_number;
    }

    public void set_message_number(long message_number) {
        this.message_number = message_number;
    }

    public int get_command_id(){
        return command_id;
    }

    public void set_command_id(int command_id){
        this.command_id = command_id;
    }

    public int get_user_id(){
        return user_id;
    }

    public void set_user_id(int user_id){
        this.user_id = user_id;
    }

    public String get_message_string(){
        return message_string;
    }

    public void set_message_string(String message_string){
        this.message_string = message_string;
    }

    public byte get_unique_identefier(){
        return unique_identefier;
    }

    public void set_unique_identefier(byte unique_identefier){
        this.unique_identefier = unique_identefier;
    }
}
