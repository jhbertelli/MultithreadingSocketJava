import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server extends Thread {
    private final Socket socket;
    private final Scanner entrada;
    private final PrintStream saida;
    private final static ArrayList<User> users = new ArrayList<>();

    public Server(final Socket socket) throws IOException {
        this.socket = socket;
        this.entrada = new Scanner(socket.getInputStream());
        saida = new PrintStream(socket.getOutputStream());
    }

    public static void main(String[] args) throws IOException {
        int port = 12345;
        var servidor = new ServerSocket(port);
        System.out.printf("Servidor iniciado na porta %d!%n", port);

        while (true) {
            var socket = servidor.accept();

            new Server(socket).start();
        }
    }

    public void run() {
        try {
            String nome = entrada.nextLine();

            System.out.printf(
                "Conexão estabelecida com o cliente: %s (IP: %s:%d)%n",
                nome,
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );

            users.add(new User(nome, socket));

            while (entrada.hasNextLine()) {
                String input = entrada.nextLine();

                String commandType = new Command(input)
                    .getType();

                switch (commandType) {
                    case Command.LIST_USERS:
                        showUsers();
                        break;
                    case Command.SEND_MESSAGE:
                        break;
                    case Command.SEND_FILE:
                        break;
                    case Command.EXIT:
                        handleSocketClosure();
                        return;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showUsers() {
        var output = new StringBuilder("Usuários conectados:\n");

        for (User user : users) {
            output
                .append(user.getUsername())
                .append("\n");
        }

        saida.println(output);
    }

    private void handleSocketClosure() throws IOException {
        var user = users.stream()
            .filter(u -> u.getSocket().equals(socket))
            .findFirst()
            .get();

        socket.close();
        entrada.close();
        saida.close();

        System.out.printf(
            "Conexão encerrada com o cliente: %s (IP: %s:%d)%n",
            user.getUsername(),
            socket.getInetAddress().getHostAddress(),
            socket.getPort()
        );

        users.remove(user);
    }
}
