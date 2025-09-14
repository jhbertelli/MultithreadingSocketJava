import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {
    private final Socket socket;
    private final PrintStream saida;
    private final BufferedReader entrada;
    private final Object serverResponded = new Object();

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        saida = new PrintStream(socket.getOutputStream());
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static void main(String[] args) throws IOException {
        var socket = new Socket("localhost", 12345);
        System.out.println("Cliente conectado!");

        new Client(socket).start();
    }

    public void run() {
        try {
            var teclado = new Scanner(System.in);

            System.out.println("Digite seu nome:");

            String input = teclado.nextLine();
            saida.println(input);

            var clientListener = new ClientListener(entrada, serverResponded);
            clientListener.start();

            showMenu();

            while (!input.equals("/sair")) {
               System.out.print("Digite um comando: ");
               input = teclado.nextLine();

               String commandType = new Command(input)
                   .getType();

               switch (commandType) {
                   case "/users":
                   case "/send message":
                   case "/send file":
                   case "/sair":
                       break;
                   default:
                       System.out.print("Comando inválido. Digite um comando válido: ");
                       continue;
               }

               saida.println(input);

               synchronized (serverResponded) {
                   serverResponded.wait();
               }
            }

            saida.close();
            teclado.close();
            socket.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void showMenu() {
        System.out.println("Comandos:");
        System.out.println("/users - Lista todos os usários");
        System.out.println("/send message <destinatario> <mensagem> - Envia uma mensagem de texto para o destinatário");
        System.out.println("/send file <destinatario> <caminho do arquivo> - Envia um arquivo para o destinatário");
        System.out.println("/sair - Termina a sessão e sai do chat");
    }
}
