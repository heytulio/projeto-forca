package forca;

import java.text.Normalizer;
import java.util.*;

public class Maestro {

    private static List<String> palavras = new ArrayList<>();
    private String palavraSelecionada;
    private char[] tentativa;
    private int vidas;
    private boolean isRunning;
    private Set<Character> letrasTentadas = new HashSet<>();

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
        vidas = 1;
        isRunning = true;
        letrasTentadas.clear();

        Arrays.fill(tentativa, '_');
    }

    public boolean adivinharLetra(char letra) {
        letra = Character.toLowerCase(letra);

        if (letrasTentadas.contains(letra)) {
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

        if (!acertou) {
            vidas--;
        }

        if (vidas == 0 || verificaSeCompleto()) {
            isRunning = false;
        }

        return acertou;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String getPalavraSelecionada() {
        return palavraSelecionada;
    }

    public char[] getTentativa() {
        return tentativa;
    }

    private boolean verificaSeCompleto() {
        for (char c : tentativa) {
            if (c == '_') return false;
        }
        return true;
    }

    private String normalizarTexto(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }
    
    private boolean adivinharPalavra(String palavra) {
    	if(palavra.equals(palavraSelecionada)) {
    		return true;
    	}
    	return false;
    }
}
