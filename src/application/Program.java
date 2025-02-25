package application;

import java.util.Scanner;
import forca.Maestro;

public class Program {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        Maestro forca = new Maestro();
        
        forca.adicionarPalavra("mochila", "criança", "diabo", "rede", "pá", "fone", "LUZ", "Medo");

        System.out.println("\nBem-vindo ao jogo da Forca!");
        System.out.println("\nTente adivinhar a palavra uma letra por vez.");
        System.out.println("\nVocê tem 5 vidas. Boa sorte!\n");

        boolean jogarNovamente;
        
        do {
            forca.gerarJogo();
            
            while (forca.isRunning()) {
                System.out.print("Faça seu palpite (uma única letra): ");
                
                String entrada = input.next();
                
                if (entrada.length() != 1 || !Character.isLetter(entrada.charAt(0))) {
                    System.out.println("Entrada inválida! Digite apenas uma letra.");
                    continue;
                }
                
                char palpite = entrada.toLowerCase().charAt(0);
                forca.adivinharLetra(palpite);
            }
            
            System.out.print("\nDeseja jogar novamente? (s/n): ");
            jogarNovamente = input.next().equalsIgnoreCase("s");
            
        } while (jogarNovamente);
        
        System.out.println("Obrigado por jogar! Até a próxima.");
        input.close();
    }
}

//deu certo aqui