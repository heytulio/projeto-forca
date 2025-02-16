package forca;

import java.text.Normalizer;
import java.util.*;

public class Maestro {
    
    private static List<String> palavras = new ArrayList<>();
    private String palavraSelecionada;
    private char[] tentativa;
    private int vidas;
    private boolean isRunning;
    private Set<Character> letrasTentadas = new HashSet<>(); // Armazena letras já usadas
    
    public void adicionarPalavra(String... palavras) {
        for (String s : palavras) {
            Maestro.palavras.add(normalizarTexto(s.toLowerCase()));
        }
    }
    
    public void gerarJogo() {
        if (Maestro.palavras.isEmpty()) {
            System.out.println("Sem palavras para iniciar o jogo");
            return;
        }
        
        palavraSelecionada = Maestro.palavras.get(new Random().nextInt(Maestro.palavras.size()));
        tentativa = new char[palavraSelecionada.length()];
        vidas = 5;
        isRunning = true;
        letrasTentadas.clear();
        
        Arrays.fill(tentativa, '_'); // Preenche com underscores
        
        System.out.println("A palavra tem " + palavraSelecionada.length() + " letras: " + Arrays.toString(tentativa));
    }
    
    public boolean adivinharLetra(char letra) {
        letra = Character.toLowerCase(letra);
        
        if (letrasTentadas.contains(letra)) {
            System.out.println("Você já tentou essa letra! Escolha outra.");
            return false;
        }
        
        letrasTentadas.add(letra);
        boolean acertou = false;
        
        for (int i = 0; i < palavraSelecionada.length(); i++) {
            if (letra == palavraSelecionada.charAt(i)) {
                tentativa[i] = letra;
                acertou = true;
            }
        }
        
        calculaVida(acertou);
        printPalavra();
        
        if (verificaSeCompleto()) {
            terminarJogo(true);
        } else if (vidas == 0) {
            terminarJogo(false);
        }
        
        return acertou;
    }
    
    private void calculaVida(boolean acerto) {
        if (!acerto) {
            vidas--;
            System.out.println("Letra errada! Vidas restantes: " + vidas);
        }
    }
    
    private boolean verificaSeCompleto() {
        for (char c : tentativa) {
            if (c == '_') return false;
        }
        return true;
    }
    
    private void terminarJogo(boolean venceu) {
        isRunning = false;
        if (venceu) {
            System.out.println("Parabéns! Você acertou a palavra: " + palavraSelecionada);
        } else {
            System.out.println("Você perdeu! A palavra era: " + palavraSelecionada);
        }
    }
    
    public void printPalavra() {
        System.out.println(Arrays.toString(tentativa));
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    private String normalizarTexto(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }
}
