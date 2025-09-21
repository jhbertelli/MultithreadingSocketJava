import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class Server extends Thread {
    private final Scanner entrada;
    private final PrintStream saida;
    private final User user;
    private final static ArrayList<User> users = new ArrayList<>();
    
    //alterado para pegar o nome do usuário, como esta na main
    public Server(User user) throws IOException {
        this.user = user;
        this.entrada = new Scanner(user.getSocket().getInputStream());
        saida = new PrintStream(user.getSocket().getOutputStream());
    }

    public static void main(String[] args) throws IOException {
        int port = 12345;
        var servidor = new ServerSocket(port);
        System.out.printf("Servidor iniciado na porta %d!%n", port);
        
        String logFileName = "server.log"; // adicionado

        while (true) {
            var socket = servidor.accept();
            
            //implemtação da lógica de log
            
            String clientIP = socket.getInetAddress().getHostAddress();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            
            String logEntry = String.format("Conexão estabelecida com IP: %s em %s%n", clientIP, formattedDateTime);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
                writer.write(logEntry);
            } catch (IOException e) {
            	//mensagem de erro
                System.err.println("Erro ao escrever no arquivo de log: " + e.getMessage());
            }
            
            //antes de fazer a thread, será feito a leitura do nome, e depois executar os commandos
            Scanner clienteEntrada = new Scanner(socket.getInputStream());
            String nome = clienteEntrada.nextLine();

            var user = new User(nome, socket);

            users.add(user);

            new Server(user).start();
        }
    }

    public void run() {
        try {
            var socket = user.getSocket();

            System.out.printf(
                "Conexão estabelecida com o cliente: %s (IP: %s:%d)%n", user.getUsername(),
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );

            while (entrada.hasNextLine()) {
                String input = entrada.nextLine();

                Command command = new Command(input);

                switch (command.getType()) {
                    case Command.LIST_USERS:
                        showUsers();
                        break;

                    case Command.SEND_MESSAGE:
                        sendMessage(command);
                        break;

                    case Command.SEND_FILE:
                        sendFile(command);
                        break;

                    case Command.EXIT:
                        handleSocketClosure();
                        return;
                }
            }
        } catch (IOException e) {
        	//alterado
        	System.err.println("Erro na comunicação com o cliente: " + user.getUsername());
            handleSocketClosure();
        }
    }

    private void sendMessage(Command command) throws IOException {
        String destinatario = command.getDestinatario();
        String mensagem = command.getMessage();

        User destinatarioUser = findUser(destinatario); //metodo implementado no final do bloco

        if (destinatarioUser != null) {
            PrintStream saidaDestinatario = new PrintStream(destinatarioUser.getSocket().getOutputStream());
            saidaDestinatario.printf("[MENSAGEM] <%s>: %s%n", user.getUsername(), mensagem);
            saida.println("Mensagem enviada!");
        } else {
            //Se o destinatário não for encontrado devolve uma mensagem de aviso
            saida.println("Usuário não encontrado: " + destinatario);
        }

        saida.println(ServerOperations.END_OF_OPERATION);
    }

    private void sendFile(Command command) throws IOException {
        String destinatarioArquivo = command.getDestinatario();
        String nomeArquivo = command.getFilePath();

        User userDestinatario = findUser(destinatarioArquivo);

        if (userDestinatario != null) {
            // mensagem de arquivo a caminho!
            var saidaDestinatario = new DataOutputStream(new BufferedOutputStream(userDestinatario.getSocket().getOutputStream()));
            var entradaRemetente = new DataInputStream(new BufferedInputStream(user.getSocket().getInputStream()));

            saidaDestinatario.write(String.format("%s %s %s\n", ServerOperations.RECIEVING_FILE, user.getUsername(), nomeArquivo).getBytes(StandardCharsets.UTF_8));
            saidaDestinatario.flush();

            //roteando os bytes
            try {
                byte[] buffer = new byte[8192];

                // envia o tamanho do arquivo
                int fileSize = entradaRemetente.readInt();
                saidaDestinatario.writeInt(fileSize);
                saidaDestinatario.flush();

                int bytesRead = entradaRemetente.read(buffer);

                // envia o arquivo
                saidaDestinatario.write(buffer, 0, bytesRead);
                saidaDestinatario.flush();

                // mensagem fim do envio !
                System.out.println("Arquivo " + nomeArquivo + " roteado com sucesso para " + destinatarioArquivo);
            } catch (IOException e) {
                //mensagem de erro de roteamento
                System.err.println("Erro durante o roteamento do arquivo: " + e.getMessage());
            }
        } else {
            //mensagem erro: usuário não encontrado
            saida.println("Usuário não encontrado: " + destinatarioArquivo);
        }

        saida.println(ServerOperations.END_OF_OPERATION);
    }

    private void showUsers() {
        var output = new StringBuilder("Usuários conectados:\n");

        for (User user : users) {
            output
                .append(user.getUsername())
                .append("\n");
        }

        saida.print(output);
        saida.println(ServerOperations.END_OF_OPERATION);
    }

    // método para achar usuário
    private User findUser(String username) {
        return users.stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(username))
            .findFirst()
            .orElse(null);
    }

    private void handleSocketClosure() {
    	try {
            var socket = user.getSocket();

            users.remove(user);

            System.out.printf(
                "Conexão encerrada com o cliente: %s (IP: %s:%d)%n",
                user.getUsername(),
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );

            socket.close();
            entrada.close();
            saida.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão.");
        }
    }
}
