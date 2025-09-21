public class Command {
    public static final String SEND_MESSAGE = "/send message";
    public static final String SEND_FILE = "/send file";
    public static final String LIST_USERS = "/users";
    public static final String EXIT = "/sair";

    private final String input;

    public Command(String userInput) {
        input = userInput;
    }

    public String getType() {
        var splitString = input.split(" ");

        String type = splitString[0].startsWith("/send") && splitString.length >= 2
          ? splitString[0] + " " + splitString[1]
          : splitString[0];

        return switch (type) {
          case LIST_USERS, SEND_MESSAGE, SEND_FILE, EXIT -> type;
          default -> null;
        };
    }

    public String getDestinatario() {
        var type = getType();
        var splitString = input.split(" ", 4);

        if (type == null) {
            return null;
        }

        if (type.equals(SEND_MESSAGE) || type.equals(SEND_FILE)) {
            if (splitString.length >= 3) {
                return splitString[2];
            }
        }
        
        return null;
    }

    public String getMessage() {
        var type = getType();
        var splitString = input.split(" ", 4);

        if (type == null) {
            return null;
        }

        if (type.equals(SEND_MESSAGE) && splitString.length >= 4) {
            return splitString[3];
        }

        return null;
    }

    public String getFilePath() {
        var type = getType();
        var splitString = input.split(" ", 4);

        if (type == null) {
            return null;
        }

        if (type.equals(SEND_FILE) && splitString.length >= 4) {
            return splitString[3];
        }

        return null;
    }
}
