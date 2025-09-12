public class Command {
    public static String getCommandFromUserInput(String userInput) {
        var splitString = userInput.split(" ");

        return splitString[0].startsWith("/users")
            ? splitString[0] + " " + splitString[1]
            : splitString[0];
    }
}
