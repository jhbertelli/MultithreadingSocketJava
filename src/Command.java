public class Command {
    private final String input;

    public Command(String userInput) {
        input = userInput;
    }

    public String getType() {
        var splitString = input.split(" ");

        if (splitString[0].startsWith("/send") && splitString.length >= 2) {
            return splitString[0] + " " + splitString[1];
        }

        return splitString[0];
    }
}
