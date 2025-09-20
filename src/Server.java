import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class Server extends Thread {
    private final Socket socket;
    private final Scanner entrada;
    private final PrintStream saida;
    private final String username; //adicionado
    private final static ArrayList<User> users = new ArrayList<>();
    
    //alterado para pegar o nome do usuário, como esta na main
    public Server(final Socket socket, String username) throws IOException {
        this.socket = socket;
        this.entrada = new Scanner(socket.getInputStream());
        saida = new PrintStream(socket.getOutputStream());
        this.username = username;
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
            PrintStream clienteSaida = new PrintStream(socket.getOutputStream());
            String nome = clienteEntrada.nextLine();

            users.add(new User(nome, socket));
            
//          new Server(socket).start();
            new Server(socket, nome).start();
        }
    }

    public void run() {
        try {
//            String nome = entrada.nextLine();
        		System.out.printf(
                "Conexão estabelecida com o cliente: %s (IP: %s:%d)%n",this.username,
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );
//            users.add(new User(nome, socket));
            while (entrada.hasNextLine()) {
                String input = entrada.nextLine();
                
                //alterado
                Command command = new Command(input);
                String commandType = command.getType();

                switch (commandType) {
                    case Command.LIST_USERS:
                        showUsers();
                        break;
                        
                        //implementando envio de mensagem
                    case Command.SEND_MESSAGE:
                        String destinatario = command.getDestinatario();
                        String mensagem = command.getMessage();
                        
                        if (destinatario != null && mensagem != null) {
                        	User destinatarioUser = findUser(destinatario); //metodo implementado no final do bloco
                            
////                        	 Percorre a lista de usuários para encontrar o destinatário
////                            User destinatarioUser = null;
//                        	
//                            for (User user : users) {
//                                if (user.getUsername().equalsIgnoreCase(destinatario)) {
//                                    destinatarioUser = user;
//                                    break;
//                                }
//                            }

                            // destinatário encontrado
                        	
                            if (destinatarioUser != null) {
                                PrintStream saidaDestinatario = new PrintStream(destinatarioUser.getSocket().getOutputStream());
                                saidaDestinatario.println(String.format("[%s]: %s", this.username, mensagem));
                            } else {
                                //Se o destinatário não for encontrado devolve uma mensagem de aviso
                                saida.println("Usuário não encontrado: " + destinatario);
                            }
                        } else {
                        	//formato inváilo, devolve uma mensagem de aviso
                            saida.println("Comando " + Command.SEND_MESSAGE + " inválido. Formato: /send message <destinatario> <mensagem>" );
                        }
                        break;
                        //fim da implemntação de mensagem
                     
                     //implementação da logica do envio de arquivos - o mais complexo
                    case Command.SEND_FILE:
                    	
                    	String destinatarioArquivo = command.getDestinatario();
                        String nomeArquivo = command.getFilePath();

                        User destinatarioUser = findUser(destinatarioArquivo);

                        if (destinatarioUser != null) {
                            // mensagem de arquivo a caminho!
                            PrintStream saidaDestinatario = new PrintStream(destinatarioUser.getSocket().getOutputStream());
                            saidaDestinatario.println(String.format("RECEBENDO_ARQUIVO %s %s", this.username, nomeArquivo));
                            
                            //roteando os bytes
                            try (InputStream remetenteIn = socket.getInputStream();
                                 OutputStream destinatarioOut = destinatarioUser.getSocket().getOutputStream()) {
                                
                                byte[] buffer = new byte[8192];
                                int bytesRead;

                                while ((bytesRead = remetenteIn.read(buffer)) != -1) {
                                    destinatarioOut.write(buffer, 0, bytesRead);
                                }

                                // mensagem fim do envio !
                                saidaDestinatario.println("FIM_ARQUIVO");
                                System.out.println("Arquivo " + nomeArquivo + " roteado com sucesso para " + destinatarioArquivo);

                            } catch (IOException e) {
                            	//mensagem de erro de roteamento
                                System.err.println("Erro durante o roteamento do arquivo: " + e.getMessage());
                            }
                        } else {
                        	//mensagem erro: usuário não encontrado
                            saida.println("Usuário não encontrado: " + destinatarioArquivo);
                        }
                        break;
                        //Fim da implmentação do envio de arquivos
                        
                    case Command.EXIT:
                        handleSocketClosure();
                        return;
                    default:
                        saida.println("Comando inválido. Digite /users, /send message ou /sair.");
                        break;
                }
            }
        } catch (IOException e) {
        	//alterado
        	System.err.println("Erro na comunicação com o cliente: " + this.username);
            handleSocketClosure();
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
    
    private User findUser(String username) {
        return users.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);
    }

    private void handleSocketClosure() {          //throws IOException {
    	
    	try {
            
            var userToRemove = users.stream()
                .filter(u -> u.getUsername().equals(this.username))
                .findFirst()
                .orElse(null);

            if (userToRemove != null) {
                users.remove(userToRemove);
                System.out.printf(
                    "Conexão encerrada com o cliente: %s (IP: %s:%d)%n",
                    userToRemove.getUsername(),
                    socket.getInetAddress().getHostAddress(),
                    socket.getPort()
                );
                
                socket.close();
                entrada.close();
                saida.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão.");
        }
    	
//        var user = users.stream()
//            .filter(u -> u.getSocket().equals(socket))
//            .findFirst()
//            .get();
//
//        socket.close();
//        entrada.close();
//        saida.close();
//
//        System.out.printf(
//            "Conexão encerrada com o cliente: %s (IP: %s:%d)%n",
//            user.getUsername(),
//            socket.getInetAddress().getHostAddress(),
//            socket.getPort()
//        );
//      users.remove(user);
    	
    }
    
    // método para achar usuário
}
