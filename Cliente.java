import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        String host = "127.0.0.1"; // IP do servidor
        int porta = 12345; // Porta do servidor

        try (Socket socket = new Socket(host, porta)) {
            System.out.println("Conectado ao servidor.");

            // Streams binários
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            // Comunicação com o servidor
            System.out.println("Digite o nome de usuário:");
            String username = teclado.readLine();
            out.writeUTF(username);

            System.out.println("Digite a senha:");
            String password = teclado.readLine();
            out.writeUTF(password);

            // Recebe o resultado da autenticação
            String resposta = in.readUTF();
            System.out.println("Resposta do servidor: " + resposta);

            if (resposta.contains("Bem-vindo")) {
/*                // Thread para leitura do servidor
                Thread leitorServidor = new Thread(() -> {
                    try {
                        while (true) {
                            String respostaServidor = in.readUTF(); // Bloqueia até receber resposta
                            System.out.println("Servidor: " + respostaServidor);
                        }
                    } catch (IOException e) {
                        System.out.println("Conexão com o servidor encerrada.");
                    }
                });

                leitorServidor.start(); // Inicia a thread para leitura
*/
                // Loop principal para enviar comandos
                while (true) {
                    String comando = "";
                    while (!(comando.equals("1")) && !(comando.equals("2")) && !(comando.equals("0"))) {
                        System.out.println("Escolha o valor da operação a efetuar:");
                        System.out.println("1 - Escrita");
                        System.out.println("2 - Leitura");
                        System.out.println("0 - Sair");
                        comando = teclado.readLine();
                    }
                    if (comando.equals("0")) {
                        break;
                    }

                    String comandoTipo = "";
                    while (!(comandoTipo.equals("1")) && !(comandoTipo.equals("2")) && !(comandoTipo.equals("0"))) {
                        System.out.println("Escolha o tipo da operação:");
                        System.out.println("1 - Simples");
                        System.out.println("2 - Múltipla");
                        System.out.println("0 - Sair");
                        comandoTipo = teclado.readLine();
                    }

                    out.writeUTF(comando);
                    out.writeUTF(comandoTipo);

                    if (comandoTipo.equals("0")) {
                        break;
                    }
                    if (comando.equals("1")) { // Escrita
                        if (comandoTipo.equals("1")) {
                            System.out.println("Digite a chave:");
                            String chaveTexto = teclado.readLine();
                            //byte[] chaveBytes = chaveTexto.getBytes(); // Converte texto para bytes
                            out.writeUTF(chaveTexto);

                            System.out.println("Digite o texto:");
                            String valorTexto = teclado.readLine();
                            byte[] valorBytes = valorTexto.getBytes(); // Converte texto para bytes
                            out.writeInt(valorBytes.length); // Envia o tamanho do array de bytes
                            out.write(valorBytes); // Envia o array de bytes
                        }
                        if (comandoTipo.equals("2")) {
                            System.out.println("Digite a quantidade de mensagens a enviar:");
                            String repeticoesStr = teclado.readLine();
                            int repeticoes = Integer.parseInt(repeticoesStr);
                            out.writeInt(repeticoes);
                            int i = 0;
                            while (i<repeticoes) {
                                System.out.println("Digite a chave:");
                                String chaveTexto = teclado.readLine();
                                //byte[] chaveBytes = chaveTexto.getBytes(); // Converte texto para bytes
                                //out.writeInt(chaveBytes.length); // Envia o tamanho do array de bytes
                                out.writeUTF(chaveTexto);

                                System.out.println("Digite o texto:");
                                String valorTexto = teclado.readLine();
                                byte[] valorBytes = valorTexto.getBytes(); // Converte texto para bytes
                                out.writeInt(valorBytes.length); // Envia o tamanho do array de bytes
                                out.write(valorBytes); // Envia o array de bytes

                                i++;
                            }
                        }
                    }
                    if (comando.equals("2")) { // Leitura
                        if (comandoTipo.equals("1")) {
                            System.out.println("Digite a chave:");
                            String chaveTexto = teclado.readLine();
                            //byte[] chaveBytes = chaveTexto.getBytes(); // Converte texto para bytes
                            out.writeUTF(chaveTexto);

                            int respostaTamanho = in.readInt(); // Lê o tamanho da resposta
                            if (respostaTamanho > 0) {
                                byte[] respostaBytes = new byte[respostaTamanho];
                                in.readFully(respostaBytes); // Lê os bytes da resposta
                                System.out.println("Resposta do servidor: " + new String(respostaBytes));
                            } else {
                                System.out.println("Chave não encontrada no servidor.");
                            }
                        }
                        else if (comandoTipo.equals("2")) {
                            System.out.println("Quantas chaves deseja consultar?");
                            int quantidade = Integer.parseInt(teclado.readLine());
                            out.writeInt(quantidade);

                            for (int i = 0; i < quantidade; i++) {
                                System.out.println("Digite a chave:");
                                String chave = teclado.readLine();
                                out.writeUTF(chave);
                            }

                            int tamanhoResposta = in.readInt();
                            System.out.println("Número de pares chave-valor encontrados: " + tamanhoResposta);
                            
                            for (int i = 0; i < tamanhoResposta; i++) {
                                String chave = in.readUTF();
                                int valorTamanho = in.readInt();
                                byte[] valorBytes = new byte[valorTamanho];
                                in.readFully(valorBytes);
                                System.out.println("Chave: " + chave + ", Valor: " + new String(valorBytes));
                            }
                        }
                    }
                }

                //leitorServidor.interrupt(); // Interrompe a thread de leitura ao sair
            }
        } catch (EOFException e) {
            System.out.println("Servidor fechou a conexão.");

        } catch (SocketException e) {
            System.out.println("Erro de conexão. O servidor pode ter desconectado.");
        
        }catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
