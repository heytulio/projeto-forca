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
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 9876;

        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Conectando ao servidor...");

        // envia uma mensagem de registro (JOIN) para o servidor, poderia ser um prompt caso não queiramos que o jogador ja entre diretamente
        String joinMessage = "JOIN";
        byte[] sendData = joinMessage.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(sendPacket);
        
        boolean ready = false;

        // loop pra receber mensagens do servidor
        while (true) {
            
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                // recebe a resposta do server
                clientSocket.receive(receivePacket);

                // processa a msg recebida
                String serverMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "FROM SERVER: " + serverMessage);

                // verifica coisas que o jogador tem que realizar
                if (serverMessage.contains("READY") && !ready) {
                    System.out.println("Digite 'READY' para confirmar que está pronto:");
                    String readyInput = keyboardReader.readLine();
                    if (readyInput.equalsIgnoreCase("READY")) {
                        sendMessage("READY", clientSocket, serverAddress, serverPort);
                        ready = true;
                    }
                } else if (serverMessage.startsWith("SUA VEZ")) {
                    System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Digite uma letra para adivinhar:");
                    String guess = keyboardReader.readLine();
                    sendMessage("GUESS " + guess, clientSocket, serverAddress, serverPort);
                }	else if (serverMessage.contains("Se deseja jogar novamente")) {
                    
                    System.out.println("Digite 'RESTART' se deseja reiniciar o jogo:");
                    String restartInput = keyboardReader.readLine();
                    if (restartInput.equalsIgnoreCase("RESTART")) {
                        sendMessage("RESTART", clientSocket, serverAddress, serverPort);
                    }
                }else if (serverMessage.contains("Iniciando novo jogo")) {
                    // Recebe a mensagem de reinício do servidor
                    System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                            + "O jogo foi reiniciado. Prepare-se para começar novamente!");
                    ready = false;  
                    System.out.println("Digite 'READY' para confirmar que está pronto:");
                    String readyInput = keyboardReader.readLine();
                    if (readyInput.equalsIgnoreCase("READY")) {
                        sendMessage("READY", clientSocket, serverAddress, serverPort);
                        ready = true;
                    }
                }
            }
            
            } 
        
    
    private static void sendMessage(String message, DatagramSocket socket, InetAddress address, int port) throws Exception {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        socket.send(sendPacket);
    }
}