# Sistemas Distribuídos - Servidor TCP Concorrente

Este projeto consiste na implementação de um servidor TCP em Java, concebido para lidar de forma concorrente com múltiplos clientes, além de fornecer um mecanismo robusto de autenticação de utilizadores e uma estrutura de armazenamento de dados em memória partilhada. 

## 1. Descrição Geral e Objetivos

O principal objetivo deste projeto foi aplicar conceitos avançados de Sistemas Distribuídos, nomeadamente a comunicação via *sockets* TCP e a gestão rigorosa de concorrência (*multithreading*). 

O sistema implementa os seguintes requisitos essenciais:
* **Servidor TCP Concorrente:** Capacidade de processar requisições de múltiplos clientes em simultâneo de forma assíncrona.
* **Controlo de Concorrência Sincronizado:** Garantia de acesso seguro a recursos partilhados (ficheiro de utilizadores e base de dados em memória) recorrendo a `ReentrantLock` e `Condition`.
* **Limitação de Sessões (Thread Pooling):** O servidor limita o número máximo de ligações ativas (`MAX_SESSOES = 3`). Clientes excedentes ficam em fila de espera até que uma vaga seja libertada.
* **Autenticação Persistente:** Sistema de registo e *login* com persistência num ficheiro binário (`users.bin`).

## 2. Arquitetura do Sistema

A arquitetura adota o clássico modelo **Cliente-Servidor**. Todo o peso do processamento e manutenção de estado recai sobre o servidor.

### 2.1 Lado do Servidor (`Servidor/`)

O servidor está modularizado nas seguintes classes:

* **`Servidor.java`**: A classe *Main*. Inicia o `ServerSocket` na porta 12345 e gere ativamente o número de conexões utilizando variáveis de condição (`podeConectar.await()`). Delega o tratamento de cada cliente para uma nova *thread*.
* **`ClienteHandler.java`**: O núcleo de comunicação com o cliente. Após gerir a autenticação, esta *thread* entra num ciclo infinito de processamento de comandos (operações de leitura e escrita simples, múltiplas ou condicionais).
* **`ServidorUsuarios.java`**: Gere o ciclo de vida dos utilizadores (Registo e Login). Carrega os dados do ficheiro `users.bin` para um `HashMap` no arranque e sincroniza todas as verificações de credenciais ou novos registos através de um `Lock` dedicado, garantindo a atomicidade das operações.
* **`ServidorArmazenamento.java`**: Atua como o repositório central de dados (estrutura chave-valor em bytes). Possui o seu próprio `ReentrantLock` e uma `Condition` (`dataChanged`) vital para notificar *threads* que estejam à espera de alterações no estado dos dados.

### 2.2 Lado do Cliente (`Cliente/`)

O cliente fornece uma interface via consola (CLI) para interagir com a API do servidor:

* **`Cliente.java`**: Estabelece a ligação inicial com o `127.0.0.1:12345` e lida com o menu interativo (Registo/Login e Menus de Operações). 
* **`OperationHandler.java`**: Implementa a interface `Runnable`. É instanciada numa nova *thread* sempre que o cliente solicita uma operação de I/O de dados, evitando que a consola principal do cliente bloqueie enquanto aguarda a resposta do servidor (especialmente útil em leituras condicionais). Usa os fluxos `DataInputStream` e `DataOutputStream`.

## 3. Detalhes de Implementação e Concorrência

### 3.1 Gestão de Sessões Ativas
Para evitar a sobrecarga de recursos e simular um ambiente de produção restrito, o servidor controla as entradas através da variável estática `sessoesAtuais`. Quando o limite é atingido, as novas *threads* invocam `podeConectar.await()`. Quando um cliente termina a sessão (seja por `timeout` ou por desconexão voluntária), a contagem diminui e é emitido um `podeConectar.signal()`, permitindo a entrada do próximo cliente na fila.

### 3.2 Escritas e Leituras Múltiplas (MultiPut / MultiGet)
O servidor suporta a submissão e o pedido de lotes de informação num único pedido de rede. Durante estas operações, o `Lock` do `ServidorArmazenamento` é mantido durante toda a iteração do ciclo `for`, garantindo que um bloco de escritas ou leituras múltiplas ocorre de forma totalmente atómica (isoloamento face a outros clientes).

### 3.3 Leituras Condicionais (`getWhen`)
Uma das mecânicas mais complexas do sistema. O cliente pode solicitar a leitura de uma chave (`key`) **apenas se** uma segunda chave de controlo (`keyCond`) assumir um valor específico (`valueCond`).
1. A *thread* do cliente invoca este pedido e fica em ciclo de bloqueio (`while(true)`).
2. Se a condição ainda não for verificada, a *thread* invoca `ServidorArmazenamento.getDataChangedCondition().await()`, adormecendo.
3. Sempre que qualquer outro cliente executa uma operação de escrita (`put` ou `multiPut`), o servidor emite um `signalAll()`.
4. As *threads* adormecidas acordam, reavaliam a condição e, se for verdadeira, devolvem o valor de `key` e terminam.

## 4. Como Executar

### 4.1 Iniciar o Servidor
Abra um terminal na diretoria raiz do projeto e compile os ficheiros do pacote `Servidor`:

```bash
javac Servidor/*.java
java Servidor.Servidor
```
O servidor ficará a aguardar conexões. (Nota: na primeira execução, o ficheiro `users.bin` será gerado automaticamente após o primeiro registo).

### 4.2 Iniciar o Cliente
Abra um novo terminal (pode abrir vários para simular concorrência e testar o limite máximo de sessões) e compile os ficheiros do pacote `Cliente`:

```bash
javac Cliente/*.java
java Cliente.Cliente
```

Siga as instruções no ecrã para criar uma conta (`register`), fazer `login` e começar a interagir com a base de dados distribuída.

## 5. Licença e Contexto
Projeto desenvolvido no âmbito da Unidade Curricular de Sistemas Distribuídos (CC-TP2).
