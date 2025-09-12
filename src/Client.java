import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        var socket = new Socket("localhost", 12345);
        System.out.println("Cliente conectado!");

        showMenu();

        var teclado = new Scanner(System.in);
        var saida = new PrintStream(socket.getOutputStream());

        while (socket.isConnected()) {
            String input = teclado.nextLine();

            saida.println(input);

            String commandType = new Command(input)
                .getType();

            switch (commandType) {
                case "/users":
                    break;
                case "/send message":
                    break;
                case "/send file":
                    break;
                case "/sair":
                    saida.close();
                    teclado.close();
                    socket.close();
                    System.exit(0);
                    break;
                default:
                    System.out.print("Comando inválido. Digite um comando válido: ");
            }
        }
   }

   private static void showMenu() {
       System.out.println("Comandos:");
       System.out.println("/users - Lista todos os usários");
       System.out.println("/send message <destinatario> <mensagem> - Envia uma mensagem de texto para o destinatário");
       System.out.println("/send file <destinatario> <caminho do arquivo> - Envia um arquivo para o destinatário");
       System.out.println("/sair - Termina a sessão e sai do chat");
       System.out.print("Digite um comando: ");
   }
}
