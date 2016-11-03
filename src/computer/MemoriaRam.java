package computer;

import java.util.ArrayList;

import main.Modulo;

public class MemoriaRam implements Runnable{
	public int tamanho;
	public int posicao;
	public int[] memoria;
	
	private final int NUMERO_DESSE_MODULO = 2;
	
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
			
			// verifica o sinal de controle no barramento
			int[] sinal_controle;
			
			if(Modulo.barramento.fila_controle.size() > 0){
				sinal_controle = Modulo.barramento.fila_controle.get(0);
			} else {
				sinal_controle = new int[0];
			}
			
			if(sinal_controle.length > 0){
				if (sinal_controle[1] == 0) { // GRAVAÇÃO
					int[] sinal_de_endereco = new int[2];
					if (sinal_controle[0] == 1) { // Veio da EntradaSaida (primeira metade da memória)

						int endereco_da_memoria = this.pegaPosicaoDisponivel();
						sinal_de_endereco[0] = sinal_controle[0];
						sinal_de_endereco[1] = endereco_da_memoria;
						
					} else { // Veio da CPU (segunda metade da memória) 
						
						sinal_de_endereco[0] = sinal_controle[0];
						sinal_de_endereco[1] = sinal_controle[2];
						
					}
					
					Modulo.barramento.adicionaFilaEndereco(sinal_de_endereco);
				} else { // LEITURA (veio da CPU)
					int[] sinal_de_endereco = {sinal_controle[0], sinal_controle[2]};
					int[] sinal_de_dado = {NUMERO_DESSE_MODULO, sinal_controle[2], this.memoria[sinal_controle[2]], -1, -1, -1, 0};
					
					Modulo.barramento.adicionaFilaEndereco(sinal_de_endereco);
					Modulo.barramento.adicionaFilaDado(sinal_de_dado);
				}
				
				// Consome o sinal de controle
				Modulo.barramento.fila_controle.remove(0);
				
			}
			
			
			// Verifica o sinal de dado no barramento
			int[] sinal_dado;
			
			if(Modulo.barramento.fila_dado.size() > 0){
				sinal_dado = Modulo.barramento.fila_dado.get(0);
			} else {
				sinal_dado = new int[0];
			}
			
			if (sinal_dado.length > 0 && sinal_dado[0] != NUMERO_DESSE_MODULO && sinal_dado[6] == 0) {
				
				if(sinal_dado[0] == 1) { // Veio da ES
					// Grava no início da memória RAM
					this.adicionaNoComecoDaMemoria(sinal_dado[1], sinal_dado);
				} else { // Veio da CPU
					// Grava na segunda metade da memória RAM
					this.adicionaNoFinalDaMemoria(sinal_dado[1], sinal_dado[2]);
				}
				
				// Seta a flag para "lido"
				Modulo.barramento.fila_dado.get(0)[6] = 1;
				
			}
			
		}
	}
}
