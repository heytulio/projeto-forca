package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import forca.Maestro;

public class UDPServer {

	//define o número máximo de jogadores
    private static final int MAX_PLAYERS = 4;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static Map<String, Integer> players = new HashMap<>(); // chave: "IP:Porta", Valor: Porta
    private static Maestro game = new Maestro();
    private static int currentPlayerIndex = 0;
    private static boolean gameStarted = false;

    public static void main(String args[]) throws Exception {
        int serverPort = 9876;
        DatagramSocket serverSocket = new DatagramSocket(serverPort);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "UDP server rodando na porta " + serverPort);

        game.adicionarPalavra("mochila", "criança", "diabo", "rede", "pá", "fone", "LUZ", "Medo");
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
            } else if (message.startsWith("GUESS")) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Processando mensagem GUESS...");
                handleGuess(message, remoteAddress, remotePort, serverSocket);
            } else {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Mensagem desconhecida recebida: " + message);
            }
        }
    }

    //função para lidar com a entrada de jogadores
    private static void handleJoin(InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        // cria uma chave única combinando o endereço IP e a porta pra diferenciar cada jogador, no caso de estar na mesma rede "localhost"
        String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

        // verifica se o jogador já está registrado
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

            //caso não esteja, adiciona o jogador
            players.put(playerKey, remotePort);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Novo jogador conectado: " + remoteAddress.getHostAddress() + ":" + remotePort);

            // envia uma mensagem de boas-vindas ao jogador
            String welcomeMessage = "Bem-vindo ao jogo! Aguardando outros jogadores...";
            byte[] sendData = welcomeMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
            serverSocket.send(sendPacket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Mensagem de boas-vindas enviada para " + remoteAddress.getHostAddress() + ":" + remotePort);

            // verifica se todos os jogadores estão conectados e inicia o game
            if (players.size() == MAX_PLAYERS && !gameStarted) {
                gameStarted = true;
                broadcast("Todos os jogadores conectados. O jogo vai começar!", serverSocket);
                startGame(serverSocket);
            }
        }
    }

    
    //função para tratar o palpite do jogador da vez
    private static void handleGuess(String message, InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket) throws Exception {
        char guess = message.split(" ")[1].charAt(0);
        boolean correctGuess = game.adivinharLetra(guess);

        String response = "Jogador " + remoteAddress.getHostAddress() + ":" + remotePort + " tentou a letra " + guess + ". ";
        response += correctGuess ? "Acertou!" : "Errou!";
        response += " Estado atual: " + Arrays.toString(game.getTentativa());

        broadcast(response, serverSocket);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Resposta enviada para todos os jogadores: " + response);
        
        
        //checa se já tem um jogo rodando
        if (game.isRunning()) {
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
        } else {
        	//lida com o fim do jogo e inicia um novo
            String endMessage = "Jogo terminado. A palavra era: " + game.getPalavraSelecionada();
            broadcast(endMessage, serverSocket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Jogo terminado. Mensagem enviada para todos os jogadores: " + endMessage);

            game.gerarJogo();
            broadcast("Novo jogo iniciado! Aguarde sua vez.", serverSocket);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Novo jogo iniciado. Mensagem enviada para todos os jogadores.");
        }
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
}
