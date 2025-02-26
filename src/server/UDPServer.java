package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import forca.Maestro;

public class UDPServer {

	//define o número máximo de jogadores
    private static final int MAX_PLAYERS = 2;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static Map<String, Integer> players = new HashMap<>(); // chave: "IP:Porta", Valor: Porta
    private static Map<String, Boolean> playersReady = new HashMap<>();
    private static Map<String, Boolean> playersRestart = new HashMap<>();

    private static Maestro game = new Maestro();
    private static int currentPlayerIndex = 0;
    private static boolean gameStarted = false;

    public static void main(String args[]) throws Exception {
        int serverPort = 9876;
        DatagramSocket serverSocket = new DatagramSocket(serverPort);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "UDP server rodando na porta " + serverPort);

//        game.adicionarPalavra("mochila", "criança", "diabo", "rede", "pá", "fone", "LUZ", "Medo",
//                "noite", "sombrio", "caverna", "mistério", "relâmpago", "abismo", "sombra", "escuridão",
//                "vento", "sussurro", "neblina", "suspense", "ciclo", "gótico", "perdição", "sepulcro",
//                "miragem", "eco", "corvo", "labirinto", "enigma", "pavor", "fantasma", "lua", "tormenta",
//                "pesadelo", "oculto", "bruma", "tenebroso", "crepúsculo", "assombração", "horizonte");
        game.adicionarPalavra("medo");
        game.gerarJogo();

        byte[] receivedData = new byte[1024];

        while (true) {
            Arrays.fill(receivedData, (byte) 0);
            DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);

            //aguarda uma mensagem do cliente
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Aguardando mensagem do cliente...");
            serverSocket.receive(receivePacket);

            // pega o endereço e a porta do cliente
            InetAddress remoteAddress = receivePacket.getAddress();
            int remotePort = receivePacket.getPort();

            //converte a mensagem recebida pra uma string
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Mensagem recebida de " + remoteAddress.getHostAddress() + ":" + remotePort + ": " + message);

            // processa a mensagem, logs para depuração e confirmação de conexão do cliente
            if (message.startsWith("JOIN")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem JOIN...");
                handleJoin(remoteAddress, remotePort, serverSocket);
            } else if (message.startsWith("READY")) {
                handleReady(message, remoteAddress, remotePort, serverSocket);
            } else if (message.startsWith("GUESS")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem GUESS...");
                handleGuess(message, remoteAddress, remotePort, serverSocket);
            } else if(message.toUpperCase().startsWith("READY")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem READY...");
                handleReady(remoteAddress, remotePort, serverSocket);
            } else if (message.toUpperCase().startsWith("RESTART")) {
            	System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem RESTART...");
                handleRestart(remoteAddress, remotePort, serverSocket);
            }
            	else {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Mensagem desconhecida recebida: " + message);
            }
        }
    }

    //função para lidar com a entrada de jogadores
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
            playerReadyStatus.put(playerKey, false); // Inicialmente, o jogador não está pronto
            playerLives.put(playerKey, 5); // Cada jogador começa com 5 vidas

            String welcomeMessage = "Bem-vindo ao jogo! Digite 'READY' para confirmar que está pronto. Você tem 5 vidas.";
            byte[] sendData = welcomeMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Novo jogador conectado: " + remoteAddress.getHostAddress() + ":" + remotePort);
            playersReady.put(playerKey, false);

            // envia uma mensagem de boas-vindas ao jogador
            String welcomeMessage = "Bem-vindo ao jogo! Envie 'READY' quando estiver pronto.";
            byte[] sendData = welcomeMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);

            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Mensagem de boas-vindas enviada para " + playerKey);
        }
    }
    
    //funcao para lidar com o ready dos jogadores
    private static void handleReady(InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

        if (players.containsKey(playerKey)) {
            if (!playersReady.get(playerKey)) {
                playersReady.put(playerKey, true);
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Jogador " + playerKey + " está pronto.");

                broadcast("Jogador " + playerKey + " está pronto.", serverSocket);
            }

            // ve se geral já deu ready se sim ele starta o jogos
            if (players.size() == MAX_PLAYERS && playersReady.values().stream().allMatch(ready -> ready)) {
                gameStarted = true;
                broadcast("Todos os jogadores estão prontos! O jogo vai começar.", serverSocket);
                broadcast("A palavra selecionada é: "+Arrays.toString(game.getTentativa()), serverSocket);
                startGame(serverSocket);
            }
        } else {
            String errorMessage = "Comando inválido. Digite 'READY' para confirmar que está pronto.";
            byte[] sendData = errorMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);
        }
    }
    

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
    
    //função para tratar o palpite do jogador da vez
    private static void handleGuess(String message, InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

        // Verifica se o jogador ainda está no jogo
        if (!players.containsKey(playerKey) || !playerLives.containsKey(playerKey)) {
            String errorMessage = "Você não está mais no jogo. Aguarde o próximo jogo.";
            byte[] sendData = errorMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Vez passada para " + nextPlayerKey);
        } else {
        	//lida com o fim do jogo e inicia um novo
        	String endMessage = "Jogo terminado. A palavra era: " + game.getPalavraSelecionada();
            broadcast(endMessage, serverSocket);
            broadcast("Se deseja jogar novamente, envie 'RESTART'.", serverSocket);
            playersRestart.clear(); 

        }
    }

    private static void passarVez(DatagramSocket serverSocket) throws Exception {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        String nextPlayerKey = (String) players.keySet().toArray()[currentPlayerIndex];
        int nextPort = players.get(nextPlayerKey);
        InetAddress nextPlayerAddress = InetAddress.getByName(nextPlayerKey.split(":")[0]);
        String turnMessage = "SUA VEZ";
        byte[] sendData = turnMessage.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nextPlayerAddress, nextPort);
        serverSocket.send(sendPacket);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Vez passada para " + nextPlayerKey);
    }
    
    private static void reiniciarJogo(DatagramSocket serverSocket) throws Exception {
        game.gerarJogo(); // Gera uma nova palavra
        players.clear(); // Limpa a lista de jogadores
        playerReadyStatus.clear(); // Reseta o status de prontidão
        playerLives.clear(); // Limpa as vidas dos jogadores
        broadcast("Novo jogo iniciado! Aguarde sua vez.", serverSocket);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Novo jogo iniciado. Mensagem enviada para todos os jogadores.");
    }
    
    //função para iniciar o jogo uma vez que todos os jogadores estão conectados
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

    
    //função para enviar uma mensagem para todos os jogadores
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
    
    public static void gameRestart() {
        game.gerarJogo();
        gameStarted = false;
        playersReady.replaceAll((key, value) -> false);
        playersRestart.clear();
    }
}
