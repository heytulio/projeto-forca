package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import forca.Maestro;

public class UDPServer {

	private static final int MAX_PLAYERS = 2;
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static Map<String, Integer> players = new HashMap<>();
	private static Map<String, Boolean> playerReadyStatus = new HashMap<>();
	private static Map<String, Integer> playerLives = new HashMap<>();
	private static Maestro game = new Maestro();
	private static int currentPlayerIndex = 0;
	private static boolean gameStarted = false;

	public static void main(String args[]) throws Exception {
		int serverPort = 9876;
		DatagramSocket serverSocket = new DatagramSocket(serverPort);
		System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "UDP server rodando na porta " + serverPort);

		game.adicionarPalavra("mochila", "criança", "diabo", "rede", "pá", "fone", "LUZ", "Medo");
		game.gerarJogo();

		byte[] receivedData = new byte[1024];

		while (true) {
			Arrays.fill(receivedData, (byte) 0);
			DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);

			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Aguardando mensagem do cliente...");
			serverSocket.receive(receivePacket);

			InetAddress remoteAddress = receivePacket.getAddress();
			int remotePort = receivePacket.getPort();

			String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Mensagem recebida de "
					+ remoteAddress.getHostAddress() + ":" + remotePort + ": " + message);

			if (message.startsWith("JOIN")) {
				System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Processando mensagem JOIN...");
				handleJoin(remoteAddress, remotePort, serverSocket);
			} else if (message.startsWith("READY")) {
				handleReady(message, remoteAddress, remotePort, serverSocket);
			} else if (message.startsWith("GUESS")) {
				System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Processando mensagem GUESS...");
				handleGuess(message, remoteAddress, remotePort, serverSocket);
			} else {
				System.out.println(
						"[" + dtf.format(LocalDateTime.now()) + "] " + "Mensagem desconhecida recebida: " + message);
			}
		}
	}

	private static void handleJoin(InetAddress remoteAddress, int remotePort, DatagramSocket serverSocket)
			throws Exception {
		String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

		if (!players.containsKey(playerKey)) {
			if (players.size() >= MAX_PLAYERS) {
				String fullMessage = "Jogo cheio. Tente novamente mais tarde.";
				byte[] sendData = fullMessage.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
				serverSocket.send(sendPacket);
				System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Jogo cheio. Mensagem enviada para "
						+ remoteAddress.getHostAddress() + ":" + remotePort);
				return;
			}

			players.put(playerKey, remotePort);
			playerReadyStatus.put(playerKey, false);
			playerLives.put(playerKey, 5);

			String welcomeMessage = "Bem-vindo ao jogo! Digite 'READY' para confirmar que está pronto. Você tem 5 vidas.";
			byte[] sendData = welcomeMessage.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
			serverSocket.send(sendPacket);
			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Novo jogador conectado: "
					+ remoteAddress.getHostAddress() + ":" + remotePort);
		}
	}

	private static void handleReady(String message, InetAddress remoteAddress, int remotePort,
			DatagramSocket serverSocket) throws Exception {
		String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

		System.out.print("\n");
		if (message.equalsIgnoreCase("READY")) {
			playerReadyStatus.put(playerKey, true);
			String ackMessage = "Você está pronto! Aguardando outros jogadores...";
			byte[] sendData = ackMessage.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
			serverSocket.send(sendPacket);
			System.out.print("\n");
			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Jogador " + playerKey
					+ " confirmou que está pronto.");

			// Envia uma mensagem para todos os jogadores informando quantos já estão
			// prontos
			int readyPlayers = (int) playerReadyStatus.values().stream().filter(status -> status).count();
			String waitMessage = "Aguardando outros jogadores... (" + readyPlayers + "/" + MAX_PLAYERS + " prontos)";
			broadcast(waitMessage, serverSocket);

			if (playerReadyStatus.size() == MAX_PLAYERS
					&& playerReadyStatus.values().stream().allMatch(status -> status)) {
				broadcast("\nTodos os jogadores estão prontos. O jogo vai começar!", serverSocket);
				String hiddenWord = String.join(" ", Collections.nCopies(game.getPalavraSelecionada().length(), "_"));
				System.out.print("\n");

				broadcast("\nO jogo começou! A palavra é: " + hiddenWord, serverSocket);
				startGame(serverSocket);
			}
		} else {
			String errorMessage = "Comando inválido. Digite 'READY' para confirmar que está pronto.";
			byte[] sendData = errorMessage.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
			serverSocket.send(sendPacket);
		}
	}

	private static void handleGuess(String message, InetAddress remoteAddress, int remotePort,
			DatagramSocket serverSocket) throws Exception {
		String playerKey = remoteAddress.getHostAddress() + ":" + remotePort;

		if (!players.containsKey(playerKey) || !playerLives.containsKey(playerKey)) {
			String errorMessage = "Você não está mais no jogo. Aguarde o próximo jogo.";
			byte[] sendData = errorMessage.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
			serverSocket.send(sendPacket);
			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Jogador " + playerKey
					+ " tentou jogar, mas foi eliminado.");
			return;
		}

		if (message.startsWith("GUESS")) {
			if (message.split(" ").length < 2) {
				String errorMessage = "Palpite inválido. Use o formato 'GUESS <letra>'.";
				byte[] sendData = errorMessage.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);
				serverSocket.send(sendPacket);
				System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Jogador " + playerKey
						+ " enviou um palpite inválido.");
				return;
			}

			char guess = message.split(" ")[1].charAt(0);
			boolean correctGuess = game.adivinharLetra(guess);

			if (!correctGuess) {
				playerLives.put(playerKey, playerLives.get(playerKey) - 1);
			}

			System.out.print("\n");
			String response = "Jogador " + playerKey + " tentou a letra " + guess + ". ";
			System.out.print("\n");
			response += correctGuess ? "Acertou!" : "Errou!";
			response += " Estado atual: " + Arrays.toString(game.getTentativa());
			response += " Vidas restantes: " + playerLives.get(playerKey);

			broadcast(response, serverSocket);
			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
					+ "Resposta enviada para todos os jogadores: " + response);

			if (game.isRunning()) {
				if (playerLives.get(playerKey) <= 0) {
					String eliminationMessage = "Jogador " + playerKey + " perdeu todas as vidas e foi eliminado!";
					broadcast(eliminationMessage, serverSocket);
					System.out.println(
							"[" + dtf.format(LocalDateTime.now()) + "] " + "Jogador " + playerKey + " foi eliminado.");

					players.remove(playerKey);
					playerLives.remove(playerKey);

					if (players.isEmpty()) {
						String endMessage = "Todos os jogadores foram eliminados! A palavra era: "
								+ game.getPalavraSelecionada();
						broadcast(endMessage, serverSocket);
						System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
								+ "Todos os jogadores foram eliminados. Mensagem enviada para todos os jogadores: "
								+ endMessage);

						reiniciarJogo(serverSocket);
						return;
					}
				}

				passarVez(serverSocket);
			} else {
				System.out.print("\n");
				String winMessage = "Jogador " + playerKey + " completou a palavra! A palavra era: "
						+ game.getPalavraSelecionada();
				broadcast(winMessage, serverSocket);
				System.out.println(
						"[" + dtf.format(LocalDateTime.now()) + "] " + "Jogador " + playerKey + " venceu o jogo!");

				reiniciarJogo(serverSocket);
			}
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
		System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Vez passada para " + nextPlayerKey);
	}

	private static void reiniciarJogo(DatagramSocket serverSocket) throws Exception {
		System.out.println("\n=====================================");
		System.out.println(" 🎉 O JOGO TERMINOU! 🎉");
		System.out.println("=====================================\n");

		broadcast("\n🔄 O jogo terminou! Deseja jogar novamente? (s/n)", serverSocket);

		String respostaValida = "";
		while (true) {
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);

			String resposta = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim().toLowerCase();
			System.out.println("Resposta recebida: " + resposta);

			if (resposta.equals("s") || resposta.equals("n")) {
				respostaValida = resposta;
				break;
			} else {
				broadcast("\n⚠ Resposta inválida! Digite 's' para jogar novamente ou 'n' para sair.", serverSocket);
			}
		}

		if (respostaValida.equals("n")) {
			broadcast("\n❌ O jogo foi encerrado. Obrigado por jogar! 🎮", serverSocket);
			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] ❌ Jogo encerrado.");
			return;
		}

		game.gerarJogo();
		playerReadyStatus.clear();

		for (String playerKey : players.keySet()) {
			playerLives.put(playerKey, 5);
		}

		System.out.println("\n=====================================");
		System.out.println(" 🔄 REINICIANDO O JOGO PARA TODOS OS JOGADORES...");
		System.out.println("=====================================\n");

		String layoutJogo = "\n=====================================\n" + " 🎮 NOVO JOGO INICIADO! \n"
				+ " A palavra tem " + game.getPalavraSelecionada().length() + " letras.\n"
				+ "=====================================\n";

		broadcast(layoutJogo, serverSocket);
		System.out.println("[" + dtf.format(LocalDateTime.now()) + "] ✅ Novo jogo iniciado! 🎮");

		passarVez(serverSocket);
	}

	private static void startGame(DatagramSocket serverSocket) throws Exception {
		currentPlayerIndex = 0;
		String firstPlayerKey = (String) players.keySet().toArray()[currentPlayerIndex];
		int firstPort = players.get(firstPlayerKey);
		InetAddress firstPlayerAddress = InetAddress.getByName(firstPlayerKey.split(":")[0]);

		String startMessage = "SUA VEZ";
		byte[] sendData = startMessage.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, firstPlayerAddress, firstPort);
		serverSocket.send(sendPacket);

		System.out.println("\n=====================================");
		System.out.println(" 🚀 O JOGO FOI INICIADO! ");
		System.out.println(" 🎮 PRIMEIRO JOGADOR: " + firstPlayerKey);
		System.out.println(" 🕹️  VEZ PASSADA PARA O PRIMEIRO JOGADOR");
		System.out.println("=====================================\n");

		System.out.println(
				"[" + dtf.format(LocalDateTime.now()) + "] ✅ Jogo iniciado. Vez passada para " + firstPlayerKey);
	}

	private static void broadcast(String message, DatagramSocket serverSocket) throws Exception {
		for (Map.Entry<String, Integer> entry : players.entrySet()) {
			String[] parts = entry.getKey().split(":");
			InetAddress address = InetAddress.getByName(parts[0]);
			int port = entry.getValue();
			byte[] sendData = message.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
			serverSocket.send(sendPacket);
			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + "Mensagem enviada para " + entry.getKey()
					+ ": " + message);
		}
	}
}

//deu certo
