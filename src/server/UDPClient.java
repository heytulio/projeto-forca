package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UDPClient {

	public static void main(String args[]) throws Exception {
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in));
		try (DatagramSocket clientSocket = new DatagramSocket()) {
			InetAddress serverAddress = InetAddress.getByName("localhost");
			int serverPort = 9876;

			System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conectando ao servidor...");

			String jogadorId = "Jogador 1";

			String joinMessage = "JOIN";
			byte[] sendData = joinMessage.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
			clientSocket.send(sendPacket);

			while (true) {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);

				String serverMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
				System.out.println(
						"\n[" + dtf.format(LocalDateTime.now()) + "] " + jogadorId + " FROM SERVER: " + serverMessage);

				if (serverMessage.contains("Digite 'READY' para confirmar que está pronto.")) {
					boolean isReady = false;
					while (!isReady) {
						System.out.println("\n[" + dtf.format(LocalDateTime.now()) + "] " + jogadorId
								+ "\nDigite 'READY' para confirmar que está pronto:");
						String readyMessage = keyboardReader.readLine().toUpperCase();

						if (readyMessage.equalsIgnoreCase("READY")) {
							sendData = readyMessage.getBytes();
							sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
							clientSocket.send(sendPacket);
							isReady = true;
						} else {
							System.out.println("\n[" + dtf.format(LocalDateTime.now()) + "] " + jogadorId
									+ " Erro: digite 'READY' corretamente.");
						}
					}
				}

				else if (serverMessage.startsWith("SUA VEZ")) {
					System.out.println("\n[" + dtf.format(LocalDateTime.now()) + "] " + jogadorId
							+ "\nDigite uma letra para adivinhar:");
					String guess = keyboardReader.readLine().trim();
					System.out.print("\n");


					String guessMessage = "GUESS " + guess;
					sendData = guessMessage.getBytes();
					sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
					clientSocket.send(sendPacket);
				}

				else if (serverMessage.contains("Deseja jogar novamente? (s/n)")) {
					String jogarNovamente;
					while (true) {
						System.out.print("\nDigite 's' para jogar novamente ou 'n' para sair: ");
						jogarNovamente = keyboardReader.readLine().trim().toLowerCase();

						if (jogarNovamente.equals("s") || jogarNovamente.equals("n")) {
							break;
						} else {
							System.out.println("⚠ Resposta inválida! Digite apenas 's' ou 'n'.");
						}
					}

					sendData = jogarNovamente.getBytes();
					sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
					clientSocket.send(sendPacket);
					
					if (jogarNovamente.equals("n")) {
						System.out.println("\nObrigado por jogar! 👋");
						break;
					}
				}
			}
		}
	}
}


//super certo