public class Command {
    private final String input;

    public Command(String userInput) {
        input = userInput;
    }

    public String getType() {
        var splitString = input.split(" ");

        return splitString[0].startsWith("/users")
            ? splitString[0] + " " + splitString[1]
            : splitString[0];
    }
}
