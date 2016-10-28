package computer;

import java.util.ArrayList;

import main.Modulo;

public class Barramento {

	public int largura, frequencia;
	
	// (origem, destino, ação)
	ArrayList<int[]> fila_controle = new ArrayList<int[]>();
	int contador_leitura_controle = 0;
	int contador_escrita_controle = 0;
	
	// (destino, endereco)
	ArrayList<int[]> fila_endereco = new ArrayList<int[]>();
	int contador_leitura_endereco = 0;
	int contador_escrita_endereco = 0;

	// (origem, comando, param1, param2, param3)
	ArrayList<int[]> fila_dado = new ArrayList<int[]>();
	int contador_leitura_dado = 0;
	int contador_escrita_dado = 0;

	/**
	 * Inicia a classe Barramento parametrizando os dados
	 * 
	 * @param largura
	 * @param frequencia
	 */
	public Barramento(int largura, int frequencia) {
		this.largura = largura;
		this.frequencia = frequencia;
	}

	/**
	 * Adiciona um array com o sinal de controle para o fila de controle
	 * 
	 * @param sinal_controle
	 */
	public void adicionaFilaControle(int posicao, int[] sinal_controle) {
		this.fila_controle.add(posicao, sinal_controle);
		this.somaContador("controle", "E");
	}
	
	/**
	 * Adiciona um endereço na fila de endereços
	 * 
	 * @param sinal_endereco
	 */
	public void adicionaFilaEndereco(int posicao, int destino, int endereco) {
		int[] sinal = {destino, endereco};
		this.fila_endereco.add(posicao, sinal);
		this.somaContador("endereco", "E");
	}
	
	/**
	 * Adiciona um array de instruções na fila de dado
	 * 
	 * @param sinal_dado
	 */
	public void adicionaFilaDado(int posicao, int[] sinal_dado) {
		for (int i = 0; i < sinal_dado.length; i++) {
			System.out.println(posicao+" -> "+sinal_dado[i]);
		}
		this.fila_dado.add(posicao, sinal_dado);
		this.somaContador("dado", "E");
		Modulo.cpu.avancaCi();
	}

	/**
	 * Avança o contado para leitura da fila parametrizada
	 * 
	 * @param contador
	 */
	public void somaContador(String contador, String tipo) {
		switch(contador){
		case "controle":
			if(tipo.equals("L")){
				this.contador_leitura_controle++;
			} else {
				this.contador_escrita_controle++;
			}
			break;
		case "endereco":
			if(tipo.equals("L")){
				this.contador_leitura_endereco++;
			} else {
				this.contador_escrita_endereco++;
			}
			break;
		case "dado":
			if(tipo.equals("L")){
				this.contador_leitura_dado++;
			} else {
				this.contador_escrita_dado++;
			}
			break;
		}
	}
	
}
