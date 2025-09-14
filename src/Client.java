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

    public Client(Socket socket, PrintStream saida) throws IOException {
        this.socket = socket;
        this.saida = saida;
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static void main(String[] args) throws IOException {
        String nome = getNome();

        var socket = new Socket("localhost", 12345);

        var saida = new PrintStream(socket.getOutputStream());
        saida.println(nome);

        new Client(socket, saida).start();
        System.out.println("Cliente conectado!");
    }

    public void run() {
        try {
            var teclado = new Scanner(System.in);

            var clientListener = new ClientListener(entrada, serverResponded);
            clientListener.start();

            showMenu();

            String userInput = "";

            while (!userInput.equals("/sair")) {
               System.out.print("Digite um comando: ");
               userInput = teclado.nextLine();

               String commandType = new Command(userInput)
                   .getType();

               if (commandType == null) {
                   System.out.print("Comando inválido. ");
                   continue;
               }

               saida.println(userInput);

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

    private static String getNome() {
        System.out.print("Digite seu nome: ");

        Scanner scanner = new Scanner(System.in);
        String nome = scanner.nextLine();

        while (nome.isBlank()) {
            System.out.print("Seu nome não pode ser vazio. Por favor, digite um nome: ");
            nome = scanner.nextLine();
        }

        return nome;
    }
}
