import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server extends Thread {
    private final Socket socket;
    private static ArrayList<User> users = new ArrayList<>();

    public Server(final Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) throws IOException {
        int port = 12345;
        var servidor = new ServerSocket(port);
        System.out.printf("Servidor iniciado na porta %d!%n", port);

        while (true) {
            var socket = servidor.accept();

            Scanner saida = new Scanner(socket.getInputStream());
            String nome = saida.nextLine();

            System.out.printf(
                "Conexão estabelecida com o cliente: %s (IP: %s:%d)%n",
                nome,
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );

            new Server(socket).start();
            users.add(new User(nome, socket));
        }
    }

    public void run() {
        try {
            Scanner entrada = new Scanner(socket.getInputStream());
            PrintStream saida = new PrintStream(socket.getOutputStream());

            while (entrada.hasNextLine()) {
                String input = entrada.nextLine();

                String commandType = new Command(input)
                    .getType();

                switch (commandType) {
                    case Command.LIST_USERS:
                        showUsers(saida);
                        break;
                    case Command.SEND_MESSAGE:
                        break;
                    case Command.SEND_FILE:
                        break;
                    case Command.EXIT:
                        handleSocketClosure(socket);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void showUsers(PrintStream saida) {
        var output = new StringBuilder("Usuários conectados:\n");

        for (User user : users) {
            output
                .append(user.getUsername())
                .append("\n");
        }

        saida.println(output);
    }

    private void handleSocketClosure(Socket socket) throws IOException {
        var user = users.stream()
            .filter(u -> u.getSocket().equals(socket))
            .findFirst()
            .get();

        socket.close();

        System.out.printf(
            "Conexão encerrada com o cliente: %s (IP: %s:%d)%n",
            user.getUsername(),
            socket.getInetAddress().getHostAddress(),
            socket.getPort()
        );

        users.remove(user);
    }
}
