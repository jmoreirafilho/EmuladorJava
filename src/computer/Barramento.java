package computer;

import java.util.ArrayList;

import main.Modulo;

public class Barramento implements Runnable {

	public int largura, frequencia;
	
	// (origem, destino, ação, endereço)
	ArrayList<int[]> fila_controle = new ArrayList<int[]>();
	int contador_leitura_controle = 0;
	int contador_escrita_controle = 0;
	
	// (origem, destino, endereco)
	ArrayList<int[]> fila_endereco = new ArrayList<int[]>();
	int contador_leitura_endereco = 0;
	int contador_escrita_endereco = 0;

	// (origem, endereco, comando, param1, param2, param3, destino)
	// ou
	// (destino, valor)
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
	public void adicionaFilaControle(int[] sinal_controle) {
		this.fila_controle.add(sinal_controle);
		this.somaContador("controle", "E");
	}
	
	/**
	 * Adiciona um endereço na fila de endereços
	 * 
	 * @param sinal_endereco
	 */
	public void adicionaFilaEndereco(int[] sinal_endereco) {
		this.fila_endereco.add(sinal_endereco);
		this.somaContador("endereco", "E");
	}
	
	/**
	 * Adiciona um array de instruções na fila de dado
	 * 
	 * @param sinal_dado
	 */
	public void adicionaFilaDado(int[] sinal_dado) {
		this.fila_dado.add(sinal_dado);
		this.somaContador("dado", "E");
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

	/**
	 * 0 -> Fila de Controle
	 * 1 -> Fila de Endereços
	 * 2 -> Fila de Dados
	 * 
	 * @param fila
	 * @return TRUE caso haja alguem na fila desejada e FALSE caso contrário
	 */
	public boolean temSinal(int fila) {
		switch (fila) {
		case 0:
			return (this.fila_controle.size() > 0 && this.fila_controle.get(0).length > 0)?true:false;
		case 1:
			return (this.fila_endereco.size() > 0 && this.fila_endereco.get(0).length > 0)?true:false;
		case 2:
			return (this.fila_dado.size() > 0 && this.fila_dado.get(0).length > 0)?true:false;
		}
		return false;
	}

	/**
	 * 0 -> Fila de Controle
	 * 1 -> Fila de Endereços
	 * 2 -> Fila de Dados
	 * 
	 * @param fila
	 * @param posicao
	 * @return TRUE caso haja alguem na fila desejada e FALSE caso contrário
	 */
	public boolean temSinal(int fila, int posicao) {
		switch (fila) {
			case 0:
				return (this.fila_controle.size() > posicao && this.fila_controle.get(0).length > 0)?true:false;
			case 1:
				return (this.fila_endereco.size() > posicao && this.fila_endereco.get(0).length > 0)?true:false;
			case 2:
				return (this.fila_dado.size() > posicao && this.fila_dado.get(0).length > 0)?true:false;
		}
		return false;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Verifica fila de controle
			this.verificaFilaControle();
			
			// Verifica fila de endereços
			this.verificaFilaEnderecos();
			
			// Verifica fila de dados
			this.verificaFilaDados();
		}
	}

	private void verificaFilaDados() {
		if (this.fila_dado.size() > 0) {
			int[] sinal_dado = this.fila_dado.get(0);
			int destino;
			if (sinal_dado.length == 2) { // é um valor que vai pra CPU
				destino = sinal_dado[0];
			} else {
				destino = sinal_dado[6];
			}
			switch (destino) {
			case 2:
				Modulo.memoria_ram.recebeDado(sinal_dado);
				break;
			case 3:
				Modulo.cpu.recebeDado(sinal_dado);
				break;
			}
			this.fila_dado.remove(0);
		}
	}

	private void verificaFilaEnderecos() {
		if(this.fila_endereco.size() > 0){
			int[] sinal_endereco = this.fila_endereco.get(0); 
			switch (sinal_endereco[0]) { // Verifica o destino e despacha
			case 1: // ES
				Modulo.entrada_saida.recebeEndereco(sinal_endereco);
				break;
			case 3: // CPU
				Modulo.cpu.recebeEndereco(sinal_endereco);
				break;
			}
			// Desenfileira esse cara que já foi utilizado
			this.fila_endereco.remove(0);
		}
	}

	private void verificaFilaControle() {
		if(this.fila_controle.size() > 0){
			// Sinal de controle sempre é pra RAM
			Modulo.memoria_ram.recebeControle(this.fila_controle.get(0));
			
			// Desenfileira esse cara que já foi utilizado
			this.fila_controle.remove(0);
		}
	}
	
}
