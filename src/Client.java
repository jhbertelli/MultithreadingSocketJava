import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {
        var socket = new Socket("localhost", 12345);
        System.out.println("Cliente conectado!");

        var teclado = new Scanner(System.in);
        var saida = new PrintStream(socket.getOutputStream());
        var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        System.out.println("Digite seu nome:");

        String input = teclado.nextLine();
        saida.println(input);

        Object serverResponded = new Object();

        var clientListener = new Thread(() -> {
            try {
                while (true) {
                    String response = entrada.readLine();
                    System.out.println(response);

                    if (response.isBlank()) {
                        synchronized (serverResponded) {
                            serverResponded.notify();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException();
            }
        });

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
   }

   private static void showMenu() {
       System.out.println("Comandos:");
       System.out.println("/users - Lista todos os usários");
       System.out.println("/send message <destinatario> <mensagem> - Envia uma mensagem de texto para o destinatário");
       System.out.println("/send file <destinatario> <caminho do arquivo> - Envia um arquivo para o destinatário");
       System.out.println("/sair - Termina a sessão e sai do chat");
   }
}
