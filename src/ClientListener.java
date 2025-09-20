import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientListener extends Thread {
    private final BufferedReader entrada;
//    private final Object serverResponded;
    private final Socket socket;
    
//    public ClientListener(BufferedReader entrada, Object serverResponded) {
//        this.entrada = entrada;
//        this.serverResponded = serverResponded;
    
    public ClientListener(Socket socket) throws IOException {
        this.socket = socket;
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void run() {
        try {
            while (true) {
                String response = entrada.readLine();

//                if (response == null || response.isBlank()) {
//                    synchronized (serverResponded) {
//                        serverResponded.notify();
//                    }
                if (response == null) {
                    System.out.println("Conexão com o servidor perdida.");
                    break;
                }
//                if (response == null) break;
//                System.out.println(response);
                
                
                //inicio da implemtação da lógica para receber arquivos
                
                if (response.startsWith("RECEBENDO_ARQUIVO")) {
                    String[] parts = response.split(" ");
                    String remetente = parts[1];
                    String nomeArquivo = parts[2];
                    
                    System.out.println("Recebendo arquivo de " + remetente + ": " + nomeArquivo);

                    try (FileOutputStream fileOut = new FileOutputStream(nomeArquivo)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        
                        //loop, até parar de enviar dados
                        while ((bytesRead = socket.getInputStream().read(buffer)) != -1) {
                            fileOut.write(buffer, 0, bytesRead);
                        }
                    }
                    // mensagem de sucesso
                    System.out.println("Transferência de arquivo concluída! Arquivo salvo como: " + nomeArquivo);
                } else {
                    //se comporta como mensagem de texto
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
