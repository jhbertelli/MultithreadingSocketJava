import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class Client extends Thread {
    private final Socket socket;
    private final PrintStream saida;
    private final BufferedReader entrada;
    private final Object serverResponded = new Object();
    private final String nomeUsuario;

    public Client(Socket socket, PrintStream saida, String nomeUsuario) throws IOException {
        this.socket = socket;
        this.saida = saida;
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.nomeUsuario = nomeUsuario;
    }

    public static void main(String[] args) throws IOException {
        String nomeUsuario = getNome();

        var socket = new Socket("localhost", 12345);

        var saida = new PrintStream(socket.getOutputStream());
        saida.println(nomeUsuario);

        new Client(socket, saida, nomeUsuario).start();
        System.out.println("Cliente conectado!");
    }

    public void run() {
        try {
            var teclado = new Scanner(System.in);

            var clientListener = new ClientListener(socket, serverResponded);
            clientListener.start();

            showMenu();

            String userInput = "";

            while (!userInput.equals(Command.EXIT)) {
               userInput = teclado.nextLine();

               Command command = new Command(userInput);
               String commandType = command.getType();

               if (commandType == null) {
                   System.out.println("Comando inválido. ");
                   continue;
               }

               if (Command.SEND_MESSAGE.equals(commandType)) {
                   String destinatario = command.getDestinatario();
                   String mensagem = command.getMessage();

                   if (destinatario == null || mensagem == null) {
                       //formato inváilo, devolve uma mensagem de aviso
                       System.out.println("Comando " + Command.SEND_MESSAGE + " inválido. Formato: /send message <destinatario> <mensagem>" );
                       continue;
                   }

                   if (Objects.equals(destinatario, nomeUsuario)) {
                       System.out.println("Não é possível enviar uma mensagem para si mesmo.");
                       continue;
                   }
               }

               // envio de aquivos
               if (Command.SEND_FILE.equals(commandType)) {
                   String destinatario = command.getDestinatario();
                   String filePath = command.getFilePath();

                   if (destinatario != null && filePath != null) {
                       if (destinatario.equals(nomeUsuario)) {
                           System.out.println("Não é possível enviar um arquivo para si mesmo.");
                           continue;
                       }

                       File file = new File(filePath);

                       if (!file.exists() || !file.isFile()) {
                           System.out.println("Erro: Arquivo não encontrado ou inválido.");
                           continue;
                       }

                       // mensagem de aviso!
//                       saida.flush();
                       try (FileInputStream fileIn = new FileInputStream(file)) {
                           byte[] buffer = new byte[8192];
                           int fileSize = (int) file.length();

                           DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                           // envia mensagem de arquivo a caminho
                           dos.write(String.format("%s %s %s\n", Command.SEND_FILE, destinatario, file.getName()).getBytes(StandardCharsets.UTF_8));
                           dos.flush();

                           // envia o tamanho do arquivo
                           dos.writeInt(fileSize);
                           dos.flush();

                           // envia o arquivo
                           int bytesRead;
                           while ((bytesRead = fileIn.read(buffer)) != -1) {
                               dos.write(buffer, 0, bytesRead);
                           }
                           dos.flush();

                           aguardarRespostaServidor();
                           System.out.println("Arquivo " + file.getName() + " enviado para " + destinatario + "!");
                       }
                   } else {
                       //mensagem de erro do formato!
                       System.out.println("Comando /send file inválido. Formato: /send file <destinatario> <caminho do arquivo>");
                   }
               } else {
                   saida.println(userInput);
                   saida.flush();
                   aguardarRespostaServidor();
               }
           }

           saida.close();
           teclado.close();
           socket.close();
       } catch (IOException | InterruptedException e) {
           throw new RuntimeException(e);
       }
    }

    private void aguardarRespostaServidor() throws InterruptedException {
        synchronized (serverResponded) {
            serverResponded.wait();
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
