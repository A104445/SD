import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class ClienteHandler implements Runnable {
    private final Socket socket;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            // Autenticação simples
            String username = in.readUTF();
            String password = in.readUTF();

            if (autenticar(username, password)) {
                out.writeUTF("Autenticação bem-sucedida. Bem-vindo!");
                System.out.println("Cliente autenticado: " + username);

                // Após autenticação, processa comandos do cliente
                while(true){
                    processarComandos(in, out);
                }
            } else {
                out.writeUTF("Autenticação falhou. Conexão encerrada.");
                System.out.println("Falha na autenticação para: " + username);
            }
        } catch (IOException e) {
            System.out.println("Erro no cliente: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar o socket: " + e.getMessage());
            }
        }
    }

    private void processarComandos(DataInputStream in, DataOutputStream out) throws IOException {
        while (true) {
            String comando = in.readUTF();
            String comandoTipo = in.readUTF();

            if (comando.equals("1")) { // Escrita
                if (comandoTipo.equals("1")) { // Simples
                    String chave = in.readUTF(); // Lê a chave
                    int tamanho = in.readInt(); // Lê o tamanho do array de bytes
                    byte[] valor = new byte[tamanho];
                    in.readFully(valor); // Lê os bytes do cliente
    
                    // Lock para garantir a segurança do acesso ao armazenamento
                    Lock lock = ServidorArmazenamento.getLock();
                    lock.lock(); // Adquire o lock
                    try {
                        ServidorArmazenamento.getArmazenamento().put(chave, valor);
                    } finally {
                        lock.unlock(); // Libera o lock
                    } 

                    //out.writeUTF("Par chave-valor armazenado com sucesso.");
                    System.out.println("Escrita: [" + chave + "] = " + new String(valor));
                }

                else if (comandoTipo.equals("2")) { // Multipla
                    int repeticoes = in.readInt();

                    for (int i = 0; i < repeticoes; i++){
                        String chave = in.readUTF(); // Lê a chave
                        int tamanho = in.readInt(); // Lê o tamanho do array de bytes
                        byte[] valor = new byte[tamanho];
                        in.readFully(valor); // Lê os bytes do cliente
                        
                        // Lock para garantir a segurança do acesso ao armazenamento
                        Lock lock = ServidorArmazenamento.getLock();
                        lock.lock(); // Adquire o lock
                        try {
                            ServidorArmazenamento.getArmazenamento().put(chave, valor);
                        } finally {
                            lock.unlock(); // Libera o lock
                        } 

                        //out.writeUTF("Par chave-valor armazenado com sucesso.");
                        System.out.println("Escrita: [" + chave + "] = " + new String(valor));
                    }
                }
            }
            if (comando.equals("2")) { // Leitura
                if (comandoTipo.equals("1")) { // Simples
                    String chave = in.readUTF(); // Lê a chave

                    // Lock para garantir segurança no acesso ao armazenamento
                    Lock lock = ServidorArmazenamento.getLock();
                    lock.lock(); // Adquire o lock
                    try {
                        byte[] valor = ServidorArmazenamento.getArmazenamento().get(chave);
                        if (valor == null) {
                            out.writeInt(0); // Envia tamanho 0 para indicar que a chave não existe
                            System.out.println("Leitura: Chave não encontrada - " + chave);
                        } else {
                            out.writeInt(valor.length); // Envia o tamanho do valor
                            out.write(valor); // Envia o valor como byte[]
                            System.out.println("Leitura: [" + chave + "] = " + new String(valor));
                        }
                    } finally {
                        lock.unlock();
                    }
                }
                else if (comandoTipo.equals("2")) { // Leitura composta
                    int repeticoes = in.readInt(); // Número de chaves fornecidas pelo cliente~
                    Map<String, byte[]> resultados = new HashMap<>(); // Armazena os pares chave-valor

                    for (int i = 0; i < repeticoes; i++) {
                        String chave = in.readUTF(); // Lê a chave enviada pelo cliente
                    
                        // Lock para garantir acesso seguro ao armazenamento
                        Lock lock = ServidorArmazenamento.getLock();
                        lock.lock();
                        try {
                            byte[] valor = ServidorArmazenamento.getArmazenamento().get(chave); // Busca o valor
                            if (valor != null) {
                                resultados.put(chave, valor); // Adiciona ao mapa se a chave existe
                            } else {
                                System.out.println("Leitura: Chave não encontrada - " + chave);
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                
                    // Enviar os resultados ao cliente
                    out.writeInt(resultados.size()); // Envia o número de pares encontrados
                    for (Map.Entry<String, byte[]> entrada : resultados.entrySet()) {
                        out.writeUTF(entrada.getKey()); // Envia a chave
                        byte[] valor = entrada.getValue();
                        out.writeInt(valor.length); // Envia o tamanho do valor
                        out.write(valor); // Envia os bytes do valor
                    }
                }

            }

            if (comando.equals("0")) {
                break;
            }
        }
    }

    private boolean autenticar(String username, String password) {
        return "admin".equals(username) && "1234".equals(password);
    }
}
