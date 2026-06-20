package practice;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Command {
    @JsonProperty("command_type")
    private int command_type; //1 - balance; 2 - deduction; 3 - addition.
    @JsonProperty("amount")
    private int amount;

    public Command() {}
    public Command(int command_type, int amount) {
        this.command_type = command_type;
        this.amount = amount;
    }

    public int get_command_type() { return command_type; }
    public void set_command_type(int command_type) { this.command_type = command_type; }
    public int get_amount() { return amount; }
    public void set_amount(int amount) { this.amount = amount; }
}