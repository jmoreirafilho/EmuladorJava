package computer;

import java.util.ArrayList;

import main.Modulo;

public class MemoriaRam implements Runnable {
	public int tamanho;
	public int posicao = 0;
	private int posicao_atual = 0;
	public int[] memoria;

	private final int NUMERO_DESSE_MODULO = 2;

	private boolean pode_mandar_endereco_pra_es = false;
	private boolean pode_mandar_endereco_pra_cpu = false;
	private boolean pode_mandar_dado_pra_cpu = false;
	private boolean primeiro_loop = true;

	private ArrayList<int[]> controle_cpu = new ArrayList<int[]>();
	private ArrayList<int[]> controle_es = new ArrayList<int[]>();
	private boolean adicionar_um_devido_ao_loop = false;
	
	public MemoriaRam(int tamanho) {
		this.tamanho = tamanho;
		this.memoria = new int[tamanho];

		for (int i = 0; i < tamanho; i++) {
			this.memoria[i] = -1;
		}
	}

	public int pegaPosicaoDisponivel() {
		if (this.memoria[this.posicao] != -1) {
			this.posicao += 4;
		}

		if (this.posicao >= (this.memoria.length / 2)) {
			this.posicao = 0;
		}

		if (this.memoria[this.posicao] == -1) {
			this.esvaziaMemoria();
		}

		return this.posicao;
	}

	private int pegaProximoEndereco(int i) {
		this.posicao_atual = this.posicao + (i * 4);
		int metade = (this.memoria.length / 2);
		if (this.posicao_atual >= metade) {
			this.posicao_atual -= metade;
		}
		return this.posicao_atual;
	}

	private void esvaziaMemoria() {
		this.memoria[this.posicao] = -1;
		this.memoria[this.posicao + 1] = -1;
		this.memoria[this.posicao + 2] = -1;
		this.memoria[this.posicao + 3] = -1;
	}

	private void adicionaNoComecoDaMemoria(int posicao, int[] instrucao) {
		this.memoria[posicao] = instrucao[2];
		this.memoria[posicao + 1] = instrucao[3];
		this.memoria[posicao + 2] = instrucao[4];
		this.memoria[posicao + 3] = instrucao[5];
		if (instrucao[2] == 7) { // loop
			this.memoria[posicao + 4] = instrucao[6];
		}
		this.posicao = posicao;
	}

	private void adicionaNoFinalDaMemoria(int posicao, int valor) {
		System.out.println("RAM: adicionou "+valor+" na posicao "+posicao);
		this.memoria[posicao] = valor;
		this.pode_mandar_endereco_pra_cpu = false;
		this.pode_mandar_dado_pra_cpu = false;
		this.controle_cpu.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Manda Endereco
			if (this.pode_mandar_endereco_pra_es) { // manda endereco pra ES
				for (int i = 0; i < Modulo.barramento.numero_de_instrucoes_passadas; i++) {
					int endereco = 0;
					if (this.primeiro_loop) {
						endereco = this.pegaPosicaoDisponivel();
					} else {
						endereco = this.pegaProximoEndereco(i);
					}
					if (this.adicionar_um_devido_ao_loop) {
						endereco++;
					}
					
					if (this.controle_es.get(0)[2] == 2) {// é um loop
						this.adicionar_um_devido_ao_loop = true;
					}

					System.out.println("RAM: mandou sinal de endereco pra ES (" + endereco + ")");

					int[] sinal_endereco_es = { NUMERO_DESSE_MODULO, 1, endereco };
					
					Modulo.barramento.ultima_posicao_inserida = endereco;
					
					Modulo.barramento.adicionaFilaEndereco(sinal_endereco_es);
					this.primeiro_loop = false;
					this.controle_es.remove(0);
				}
				this.pode_mandar_endereco_pra_es = false;
				this.primeiro_loop = true;
			}

			if (this.pode_mandar_endereco_pra_cpu) { // manda endereco pra cpu
				System.out.println("RAM: mandou sinal de endereco pra CPU");
				int[] sinal_endereco_cpu = { NUMERO_DESSE_MODULO, 3, this.controle_cpu.get(0)[3] };

				// Grava sinal de endereco
				Modulo.barramento.adicionaFilaEndereco(sinal_endereco_cpu);
				this.pode_mandar_endereco_pra_cpu = false;
			}

			if (this.pode_mandar_dado_pra_cpu && this.controle_cpu.get(0).length > 0) {
				int posicao = this.controle_cpu.get(0)[3];
				System.out.println("RAM: mandou sinal de dado pra cpu");
				if (posicao < this.memoria.length / 2) {
					if (posicao < 0) {
						posicao = (posicao * -1) - 6 + (this.tamanho / 2);
					}
					int[] inst = { NUMERO_DESSE_MODULO, posicao, this.memoria[posicao], this.memoria[posicao + 1],
							this.memoria[posicao + 2], this.memoria[posicao + 3], 3 };
					Modulo.barramento.adicionaFilaDado(inst);
				} else {
					int[] sinal_dado_pra_cpu = { 3, this.controle_cpu.get(0)[3], this.memoria[posicao] };
					Modulo.barramento.adicionaFilaDado(sinal_dado_pra_cpu);
				}
				this.pode_mandar_dado_pra_cpu = false;
				this.controle_cpu.remove(0);
			}
			
			if (this.controle_cpu.size() > 0) {
				this.pode_mandar_endereco_pra_cpu = true;
				this.pode_mandar_dado_pra_cpu = true;
			}

		}
	}

	public void recebeControle(int[] sinal_controle) {
		System.out.println("RAM: recebeu sinal de controle");
		if (sinal_controle[0] == 1) { // Veio da ES
			this.pode_mandar_endereco_pra_es = true;
			System.out.println(">>>>>>>>> "+sinal_controle[0]+", "+sinal_controle[1]+", "+sinal_controle[2]+", "+sinal_controle[3]);
			this.controle_es.add(sinal_controle);
		} else { // veio da cpu
			this.pode_mandar_endereco_pra_cpu = true;
			if (sinal_controle[2] == 0) { // Leitura
				this.pode_mandar_dado_pra_cpu = true;
			} else {
				this.pode_mandar_dado_pra_cpu = false;
			}
			this.controle_cpu.add(sinal_controle);
		}
	}

	public void recebeDado(int[] sinal_dado) {
		if (sinal_dado.length > 3) { // Veio da ES
			System.out.println("RAM: recebeu sinal de dado da ES (" + sinal_dado[1] + ")");
			this.adicionaNoComecoDaMemoria(sinal_dado[1], sinal_dado);
		} else { // Veio da CPU
			System.out.println("RAM: recebeu sinal de dado da CPU");
			this.adicionaNoFinalDaMemoria(sinal_dado[1], sinal_dado[2]);
		}
		this.imprimeMemoria();
	}

	private void imprimeMemoria() {
		for (int i = 0; i < this.memoria.length; i++) {
			System.out.print(this.memoria[i] + " | ");
			if (i == (this.memoria.length - 1)) {
				System.out.println("\n");
			}
		}
	}
}