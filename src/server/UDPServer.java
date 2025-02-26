package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import forca.Maestro;

public class UDPServer {

    // Define o número máximo de jogadores
    private static final int MAX_PLAYERS = 2;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static Map<String, Integer> players = new HashMap<>(); // Chave: "IP:Porta", Valor: Porta
    private static Map<String, Boolean> playersReady = new HashMap<>();
    private static Map<String, Boolean> playersRestart = new HashMap<>();
    private static Map<String, Integer> playerLives = new HashMap<>(); // Chave: "IP:Porta", Valor: Vidas
    private static Map<String, Boolean> playersEliminated = new HashMap<>();

    private static Maestro game = new Maestro();
    private static int currentPlayerIndex = 0;
    private static boolean gameStarted = false;

    public static void main(String args[]) throws Exception {
        int serverPort = 9876;
        DatagramSocket serverSocket = new DatagramSocket(serverPort);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "UDP server rodando na porta " + serverPort);

        game.adicionarPalavra("medo");
        game.gerarJogo();

        byte[] receivedData = new byte[1024];

        while (true) {
            Arrays.fill(receivedData, (byte) 0);
            DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);

            // Aguarda uma mensagem do cliente
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Aguardando mensagem do cliente...");
            serverSocket.receive(receivePacket);

            // Pega o endereço e a porta do cliente
            InetAddress remoteAddress = receivePacket.getAddress();
            int remotePort = receivePacket.getPort();

            // Converte a mensagem recebida para uma string
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Mensagem recebida de " + remoteAddress.getHostAddress() + ":" + remotePort + ": " + message);

            // Processa a mensagem
            if (message.startsWith("JOIN")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem JOIN...");
                handleJoin(remoteAddress, remotePort, serverSocket);
            } else if (message.startsWith("READY")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem READY...");
                handleReady(remoteAddress, remotePort, serverSocket);
            } else if (message.startsWith("GUESS")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem GUESS...");
                handleGuess(message, remoteAddress, remotePort, serverSocket);
            } else if (message.toUpperCase().startsWith("RESTART")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem RESTART...");
                handleRestart(remoteAddress, remotePort, serverSocket);
            } else {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Mensagem desconhecida recebida: " + message);
            }
        }
    }

    // Função para lidar com a entrada de jogadores
    private static void handleJoin(InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

        if (!players.containsKey(playerKey)) {
            if (players.size() >= MAX_PLAYERS) {
                String fullMessage = "Jogo cheio. Tente novamente mais tarde.";
                byte[] sendData = fullMessage.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
                serverSocket.send(sendPacket);
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Jogo cheio. Mensagem enviada para " + remoteAddress.getHostAddress() + ":" + remotePort);
                return;
            }

            players.put(playerKey, remotePort);
            playersReady.put(playerKey, false);
            playerLives.put(playerKey, 1); // Cada jogador começa com 5 vidas

            String welcomeMessage = "Bem-vindo ao jogo! Digite 'READY' para confirmar que está pronto. Você tem 5 vidas.";
            byte[] sendData = welcomeMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Novo jogador conectado: " + remoteAddress.getHostAddress() + ":" + remotePort);
        }
    }

    // Função para lidar com o ready dos jogadores
    private static void handleReady(InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

        if (players.containsKey(playerKey)) {
            if (!playersReady.get(playerKey)) {
                playersReady.put(playerKey, true);
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Jogador " + playerKey + " está pronto.");
                broadcast("Jogador " + playerKey + " está pronto.", serverSocket);
            }

            // Verifica se todos os jogadores estão prontos
            if (players.size() == MAX_PLAYERS && playersReady.values().stream().allMatch(ready -> ready)) {
                gameStarted = true;
                broadcast("Todos os jogadores estão prontos! O jogo vai começar.", serverSocket);
                broadcast("A palavra selecionada é: " + Arrays.toString(game.getTentativa()), serverSocket);
                startGame(serverSocket);
            }
        } else {
            String errorMessage = "Comando inválido. Digite 'READY' para confirmar que está pronto.";
            byte[] sendData = errorMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);
        }
    }

    // Função para lidar com o reinício do jogo
    private static void handleRestart(InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

        if (players.containsKey(playerKey)) {
            playersRestart.put(playerKey, true);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Jogador " + playerKey + " deseja reiniciar o jogo.");
            broadcast("Jogador " + playerKey + " deseja reiniciar o jogo.", serverSocket);
        }

        if (playersRestart.size() == players.size() && playersRestart.values().stream().allMatch(restart -> restart)) {
            broadcast("Todos os jogadores confirmaram! Iniciando novo jogo...", serverSocket);
            gameRestart();
        }
    }

    // Função para tratar o palpite do jogador da vez
    private static void handleGuess(String message, InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

        if (!players.containsKey(playerKey) || !playerLives.containsKey(playerKey)) {
            String errorMessage = "Você não está mais no jogo. Aguarde o próximo jogo.";
            byte[] sendData = errorMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Jogador " + playerKey + " tentou jogar, mas foi eliminado.");
            return;
        }

        if (message.startsWith("GUESS")) {
            // Verifica se o palpite é válido
            if (message.split(" ").length < 2) {
                String errorMessage = "Palpite inválido. Use o formato 'GUESS <letra>'.";
                byte[] sendData = errorMessage.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
                serverSocket.send(sendPacket);
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Jogador " + playerKey + " enviou um palpite inválido.");
                return;
            }

            char guess = message.split(" ")[1].charAt(0);
            boolean correctGuess = game.adivinharLetra(guess);

            // Atualiza as vidas do jogador se ele errar
            if (!correctGuess) {
                playerLives.put(playerKey, playerLives.get(playerKey) - 1);
            }

            String response = "Jogador " + playerKey + " tentou a letra " + guess + ". ";
            response += correctGuess ? "Acertou!" : "Errou! ";
            response += "Estado atual: " + Arrays.toString(game.getTentativa());
            response += " Vidas restantes: " + playerLives.get(playerKey);

            broadcast(response, serverSocket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Resposta enviada para todos os jogadores: " + response);

            // Verifica se a palavra foi completamente adivinhada
            if (!game.isRunning()) {
                String endMessage = "Jogo terminado. A palavra era: " + game.getPalavraSelecionada();
                broadcast(endMessage, serverSocket);
                broadcast("Se deseja jogar novamente, envie 'RESTART'.", serverSocket);
                playersRestart.clear();
            } else if (playerLives.get(playerKey) <= 0) {
                String eliminationMessage = "Jogador " + playerKey + " perdeu todas as vidas e foi eliminado!";
                broadcast(eliminationMessage, serverSocket);
                players.remove(playerKey);
                playerLives.remove(playerKey);
                passarVez(serverSocket);

                // Verifique se não há mais jogadores ativos
                if (players.isEmpty()) {
                    String gameOverMessage = "Todos os jogadores foram eliminados! Fim do jogo.";
                    broadcast(gameOverMessage, serverSocket);
                    System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + gameOverMessage);
                    // Finaliza o jogo sem reiniciar
                    return;
                }
            } else {
                passarVez(serverSocket);
            }
        }
    }


    private static void passarVez(DatagramSocket serverSocket) throws Exception {
        // Verifica se há jogadores ativos
        if (players.isEmpty()) {
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Nenhum jogador ativo. Reiniciando o jogo...");
            reiniciarJogo(serverSocket);
            return; 
        }

        // Passa a vez para o próximo jogador que ainda não foi eliminado
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            String nextPlayerKey = (String) players.keySet().toArray()[currentPlayerIndex];
            if (!playersEliminated.containsKey(nextPlayerKey)) {
                // Se o próximo jogador não foi eliminado, passa a vez
                int nextPort = players.get(nextPlayerKey);
                InetAddress nextPlayerAddress = InetAddress.getByName(nextPlayerKey.split(":")[0]);
                String turnMessage = "SUA VEZ";
                byte[] sendData = turnMessage.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nextPlayerAddress, nextPort);
                serverSocket.send(sendPacket);
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Vez passada para " + nextPlayerKey);
                break; // Sai do loop quando encontrar o próximo jogador
            }
        } while (true);
    }

    // Função para reiniciar o jogo
    private static void reiniciarJogo(DatagramSocket serverSocket) throws Exception {
        // Gera uma nova palavra
        game.adicionarPalavra("novaPalavra");
        game.gerarJogo();
        players.clear();
        playersReady.clear();
        playerLives.clear();
        playersRestart.clear();
        playersEliminated.clear();
        currentPlayerIndex = 0;
        gameStarted = false;
        broadcast("Novo jogo iniciado! Aguarde sua vez.", serverSocket);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Novo jogo iniciado. Mensagem enviada para todos os jogadores.");

        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Aguardando novos jogadores...");
    }
    
    // Função para iniciar o jogo
    private static void startGame(DatagramSocket serverSocket) throws Exception {
        currentPlayerIndex = 0;
        String firstPlayerKey = (String) players.keySet().toArray()[currentPlayerIndex];
        int firstPort = players.get(firstPlayerKey);
        InetAddress firstPlayerAddress = InetAddress.getByName(firstPlayerKey.split(":")[0]);
        String startMessage = "SUA VEZ";
        byte[] sendData = startMessage.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, firstPlayerAddress, firstPort);
        serverSocket.send(sendPacket);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Jogo iniciado. Vez passada para " + firstPlayerKey);
    }

    // Função para enviar uma mensagem para todos os jogadores
    private static void broadcast(String message, DatagramSocket serverSocket) throws Exception {
        for (Map.Entry<String, Integer> entry : players.entrySet()) {
            String[] parts = entry.getKey().split(":");
            InetAddress address = InetAddress.getByName(parts[0]);
            int port = entry.getValue();
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            serverSocket.send(sendPacket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Mensagem enviada para " + entry.getKey() + ": " + message);
        }
    }

    // Função para reiniciar o jogo
    public static void gameRestart() {
        game.gerarJogo();
        gameStarted = false;
        playersReady.replaceAll((key, value) -> false);
        playersRestart.clear();
    }
}
