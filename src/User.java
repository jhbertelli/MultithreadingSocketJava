import java.net.Socket;

public class User {
    private final String username;
    private final Socket socket;

    public User(String username, Socket socket) {
        this.username = username;
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return socket;
    }
}
