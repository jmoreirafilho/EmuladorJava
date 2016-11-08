package computer;

import java.awt.font.NumericShaper;
import java.util.ArrayList;

import main.Modulo;

public class MemoriaRam implements Runnable{
	public int tamanho;
	public int posicao;
	public int[] memoria;
	
	private final int NUMERO_DESSE_MODULO = 2;

	private boolean pode_mandar_endereco_pra_es = false;
	private boolean pode_mandar_endereco_pra_cpu = false;
	private boolean pode_mandar_dado_pra_cpu = false;
	
	private int[] controle_es;
	private int[] controle_cpu;
	
	private int[] sinal_endereco = {NUMERO_DESSE_MODULO, -1, -1};
	private int[] sinal_dado;
	
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
			
			// Manda Endereco
			if (this.pode_mandar_endereco_pra_es) { // manda endereco pra ES
				int endereco = this.pegaPosicaoDisponivel();
				
				int[] sinal_endereco_es = {NUMERO_DESSE_MODULO, 1, endereco};
				
				Modulo.barramento.adicionaFilaEndereco(sinal_endereco_es);
				
				this.pode_mandar_endereco_pra_es = false;
			}
			
			int[] instrucao = new int[4];			
			if (this.pode_mandar_endereco_pra_cpu) { // manda endereco pra cpu
				int[] sinal_endereco_cpu = {NUMERO_DESSE_MODULO, 3, this.controle_cpu[2]};

				// Grava sinal de endereco
				Modulo.barramento.adicionaFilaEndereco(sinal_endereco_cpu);

				// Prepara sinal de dado, se necessário
				if (this.controle_cpu[2] == 0 && this.pode_mandar_dado_pra_cpu) { // CPU lendo
					int posicao = this.controle_cpu[2];
					if (this.controle_cpu[2] < this.memoria.length/2) { // manda endereco de instrucao
						// Prepara sinal de dado, sendo uma instrucao.
						instrucao[0] = this.memoria[posicao];
						instrucao[1] = this.memoria[posicao + 1];
						instrucao[2] = this.memoria[posicao + 2];
						instrucao[3] = this.memoria[posicao + 3];
					} else { // Manda endereco de memoria
						// Prepara sinal de dado, sendo um valor
						instrucao[0] = -1;
						instrucao[1] = this.memoria[posicao];
						instrucao[2] = -1;
						instrucao[3] = -1;
					}
				}
				this.pode_mandar_endereco_pra_cpu = false;
			}
			
			if (this.pode_mandar_dado_pra_cpu) {
				Modulo.barramento.adicionaFilaDado(instrucao);
				this.pode_mandar_dado_pra_cpu = false;
			}

		}
	}

	public void recebeControle(int[] sinal_controle) {
		if (sinal_controle[0] == 1) { // Veio da ES
			this.pode_mandar_endereco_pra_es = true;
			this.controle_es = sinal_controle;
		} else { // veio da cpu
			this.pode_mandar_endereco_pra_cpu = true;
			if (sinal_controle[2] == 1) { // Leitura
				this.pode_mandar_dado_pra_cpu = true;
			}
			this.controle_cpu = sinal_controle;
		}
	}

	public void recebeDado(int[] sinal_dado) {
		
	}
}
