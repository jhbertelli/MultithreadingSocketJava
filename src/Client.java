import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {
    private final Socket socket;
    private final PrintStream saida;
    private final BufferedReader entrada;
//    private final Object serverResponded = new Object();

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

//            var clientListener = new ClientListener(entrada, saida); //alterado
            var clientListener = new ClientListener(socket);
            clientListener.start();

            showMenu();

            String userInput = "";

            while (!userInput.equals("/sair")) {
               System.out.print("Digite um comando: ");
               userInput = teclado.nextLine();
               
//               String commandType = new Command(userInput)
//                   .getType();
               
               Command command = new Command(userInput);
               String commandType = command.getType();
               
//               if (commandType == null) {
//                   System.out.print("Comando inválido. ");
//                   continue;
//               }
//
//               saida.println(userInput);
//
//               synchronized (serverResponded) {
//                   serverResponded.wait();
//               }
//            }
               
           // envio de aquivos
               
           if (Command.SEND_FILE.equals(commandType)) {
               String destinatario = command.getDestinatario();
               String filePath = command.getFilePath();

               if (destinatario != null && filePath != null) {
                   File file = new File(filePath);
                   if (!file.exists() || !file.isFile()) {
                       System.out.println("Erro: Arquivo não encontrado ou inválido.");
                       continue;
                   }

                   // mensagem de aviso!
                   saida.println(String.format("%s %s %s", Command.SEND_FILE, destinatario, file.getName()));
                   
                   try (FileInputStream fileIn = new FileInputStream(file)) {
                       byte[] buffer = new byte[8192];
                       int bytesRead;
                       while ((bytesRead = fileIn.read(buffer)) != -1) {
                           socket.getOutputStream().write(buffer, 0, bytesRead);
                       }
                       
                       //fim do arquivo 
                       socket.getOutputStream().flush(); 
                       System.out.println("Arquivo " + file.getName() + " enviado para " + destinatario);
                   }
               } else {
            	   //mensagem de erro do formato!
                   System.out.println("Comando /send file inválido. Formato: /send file <destinatario> <caminho do arquivo>");
                   }
               } else {
                   saida.println(userInput);
               }
           }

           saida.close();
           teclado.close();
           socket.close();
       } catch (IOException e) {
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
