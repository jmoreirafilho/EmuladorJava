package computer;

import java.util.ArrayList;

import main.Modulo;

public class MemoriaRam implements Runnable{
	public int tamanho;
	public int posicao;
	public int[] memoria;
	
	public MemoriaRam(int tamanho) {
		this.tamanho = tamanho;
		this.memoria = new int[tamanho];
		
		for(int i = 0; i < tamanho; i++){
			this.memoria[i] = -1;
		}
	}
	
	public int pegaPosicaoDisponivel() {
		if(this.memoria[this.posicao] != -1){
			this.posicao += 4;
		}
		if(this.posicao >= (this.memoria.length / 2)){
			this.posicao = 0;
		}
		if(this.memoria[this.posicao] == -1){
			this.esvaziaMemoria();
		}
		return this.posicao;
	}

	private void esvaziaMemoria() {
		this.memoria[this.posicao] = -1;
		this.memoria[this.posicao + 1] = -1;
		this.memoria[this.posicao + 2] = -1;
		this.memoria[this.posicao + 3] = -1;
	}
	
	private void adicionaNoComecoDaMemoria(int posicao, int[] instrucao) {
		this.memoria[posicao] = instrucao[1];
		this.memoria[posicao + 1] = instrucao[2];
		this.memoria[posicao + 2] = instrucao[3];
		this.memoria[posicao + 3] = instrucao[4];
	}
	
	private void adicionaNoFinalDaMemoria(int posicao, int valor) {
		this.memoria[posicao] = valor; 
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
//			this.exibeMemoria();
			this.exibeFilas();

			int posicao = Modulo.barramento.contador_leitura_controle;

			if (Modulo.barramento.fila_controle.size() > posicao && Modulo.barramento.fila_controle.get(posicao).length > 0 && Modulo.barramento.fila_controle.get(posicao)[1] == 2) {
				
				int[] fila_controle_atual = Modulo.barramento.fila_controle.get(posicao);
				
				if(fila_controle_atual.length == 2){ // Sinal de gravação
					Modulo.barramento.adicionaFilaEndereco(posicao, fila_controle_atual[0], this.pegaPosicaoDisponivel());
//					System.out.println("RAM: gravou sinal de endereço");
				} else { // Sinal de leitura
//					System.out.println(">> "+fila_controle_atual[0]+", "+fila_controle_atual[2]);
					Modulo.barramento.adicionaFilaEndereco(posicao, fila_controle_atual[0], fila_controle_atual[2]);
					int[] dado = {this.memoria[fila_controle_atual[2]]};
					Modulo.barramento.adicionaFilaDado(posicao, dado);
//					System.out.println("RAM: gravou sinal de endereço e de dado");
				}

				Modulo.barramento.somaContador("controle", "L");
			}
			
			posicao = Modulo.barramento.contador_leitura_dado;

			// Se tiver dado na fila de dados
			if (Modulo.barramento.fila_dado.size() == (posicao + 1)) {
				int posicao_memoria = Modulo.barramento.fila_endereco.get(posicao)[1];
				
				// Se a origem for ES, grava o dado no começo da memoria, passando o endereço do barramento de endereço
				if(Modulo.barramento.fila_dado.get(posicao)[0] == 1){
					this.adicionaNoComecoDaMemoria(posicao_memoria, Modulo.barramento.fila_dado.get(posicao));
//					System.out.println("RAM: gravou na memoria (ES)");
				} else {
					this.adicionaNoFinalDaMemoria(posicao_memoria, Modulo.barramento.fila_dado.get(posicao)[0]);
//					System.out.println("RAM: gravou na memoria (CPU)");
				}
				
				Modulo.barramento.somaContador("dado", "L");
			}

		}
	}

	private void exibeFilas() {
		System.out.println("--- FILAS ---");
		ArrayList<int[]> fila_controle = Modulo.barramento.fila_controle;
		if(fila_controle.size() > 0){
			for (int i = 0; i < fila_controle.size(); i++) {
				System.out.print(fila_controle.get(i)[0]+", "+fila_controle.get(i)[1]+" | ");
			}	
		}
		System.out.println("\n--------------------\n");
	}

	private void exibeMemoria() {
		System.out.println("--- Memória RAM ---");
		for (int i = 0; i < this.memoria.length; i++) {
			System.out.print(this.memoria[i]+" | ");
		}
		System.out.println("\n--------------------\n");
	}
}
