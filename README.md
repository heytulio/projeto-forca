# Projeto de Jogo da Forca

Projeto desenvolvido na Disciplina de Redes de Computadores.

Este projeto implementa o tradicional Jogo da Forca utilizando a comunicação via protocolo UDP. Um servidor gerencia o jogo, armazenando palavras e controlando as tentativas, enquanto os clientes podem se conectar para jogar, enviando letras e recebendo feedback sobre suas jogadas.

## Autores

- Emmanuel Barros Moraes [@EmmanuelBMoraes](https://github.com/EmmanuelBMoraes)
- Matheus Cavalcanti de Lima [@MatheusCavalcanti97](https://github.com/MatheusCavalcanti97)
- Samara Porto de Noronha [@samaraporto](https://github.com/samaraporto)
- Tulio Martins Vasconcelos [@heytulio](https://github.com/heytulio)

## Como iniciar o jogo em rede

Como Executar o Projeto via terminal.

### Passos:

Na pasta raiz do projeto inicie um terminal, e entre na pasta src:

`cd src/`

Compile os arquivos Java:

```
javac forca/*.java
javac server/*.java
```

Inicie o servidor em um terminal:

`java server.UDPServer`

Em outros 4 terminais, inicie os clientes:

`java server.UDPClient`

Após a execução, siga o solicitado pelo servidor.

## Como jogar

1 - O servidor inicia o jogo escolhendo uma palavra aleatória.

2 - Todos os clientes devem enviar a confirmação de que estão prontos para o jogo de fato começar.

3 - Após a confirmação, cada cliente, em sua vez, deve enviar uma letra/caracter como palpite.

4 - O jogo continua até que o jogador acerte a palavra ou esgote suas tentativas.

5 - Após o acerto da palavra, ou que todas as vidas sejam esgotadas o servidor informará quem é o vencedor ou se todos perderam e, em seguida, enviará uma solicitação para que o jogo seja reiniciado.

6 - Para o reinicio, todos os jogadores devem aceitar o pedido do servidor, assim recomeçando o ciclo.
