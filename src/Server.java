import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Scanner;

public class Server extends Thread {
    private final Socket socket;

    public Server(final Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) throws IOException {
        int port = 12345;
        var servidor = new ServerSocket(port);
        System.out.printf("Servidor iniciado na porta %d!%n", port);

        while (true) {
            var socket = servidor.accept();

            System.out.printf(
                "Conexão estabelecida com o cliente: %s:%d%n",
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );

            new Server(socket).start();
        }
    }

    public void run() {
        try {
            Scanner saida = new Scanner(socket.getInputStream());

            while (saida.hasNextLine()) {
                String input = saida.nextLine();

                String command = Command.getCommandFromUserInput(input);

                switch (command) {
                    case "/users":
                        break;
                    case "/send message":
                        break;
                    case "/send file":
                        break;
                    case "/sair":
                        handleSocketClosure(socket);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void handleSocketClosure(Socket socket) throws IOException {
        socket.close();

        System.out.printf(
            "Conexão encerrada com o cliente: %s:%d%n",
            socket.getInetAddress().getHostAddress(),
            socket.getPort()
        );
    }
}
