package computer;

import java.util.ArrayList;

import main.Modulo;

public class Cpu implements Runnable {
	public int CI = 0;
	private boolean pode_pedir_instrucao_da_ram = true;
	private boolean pode_mandar_sinal_dado = false;
	private boolean pode_processar_instrucao = false;
	private boolean estou_esperando_valores = true;
	private boolean estou_esperando_posicao_de_memoria = false;
	
	private int endereco_atual;
	private int[] instrucao_atual;
	ArrayList<Integer> valores = new ArrayList<Integer>();
	
	private int posicao_da_memoria;
	
	private int registrador_a;
	private int registrador_b;
	private int registrador_c;
	private int registrador_d;
	
	private final int NUMERO_DESSE_MODULO = 3;
	
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Processa uma instrução			
			if (this.pode_processar_instrucao) {
				this.pegaValoresDaInstrucao(this.instrucao_atual);
				if (!this.estou_esperando_valores) {
					this.processaInstrucao(this.instrucao_atual);
				}
			}
			
			// Pede Valor
			
			// Grava resultado
			
		}
	}

	private void pegaValoresDaInstrucao(int[] instrucao_atual) {
		for (int i = 3; i < 6; i++) { // Percorre os valores das instruçoes
			if (instrucao_atual[i] != -1) { // Caso seja um valor não-nulo
				if (instrucao_atual[i] < -1 && instrucao_atual[i] > -6) { // É um registrador
					switch (instrucao_atual[i]) {
					case -2: // A
						this.valores.add(this.pegaRegistradorA());
						break;
					case -3: // B
						this.valores.add(this.pegaRegistradorB());
						break;
					case -4: // C
						this.valores.add(this.pegaRegistradorC());
						break;
					case -5: // D
						this.valores.add(this.pegaRegistradorD());
						break;
					}
				} else if (instrucao_atual[i] < -5) { // Posicao de memoria

				} else { // Numero inteiro
					
				}
			}
		}
	}

	private void processaInstrucao(int[] instrucao_atual) {
		switch (instrucao_atual[2]) {
		case 1:
			// Add
			break;
		case 2:
			// Mov
			break;
		case 3:
			// Imul
			break;
		case 4:
			// Inc
			break;
		}
	}

	public int pegaRegistradorA() {
		return registrador_a;
	}

	public void defineRegistradorA(int registrador_a) {
		this.registrador_a = registrador_a;
	}

	public int pegaRegistradorB() {
		return registrador_b;
	}

	public void defineRegistradorB(int registrador_b) {
		this.registrador_b = registrador_b;
	}

	public int pegaRegistradorC() {
		return registrador_c;
	}

	public void defineRegistradorC(int registrador_c) {
		this.registrador_c = registrador_c;
	}

	public int pegaRegistradorD() {
		return registrador_d;
	}

	public void defineRegistradorD(int registrador_d) {
		this.registrador_d = registrador_d;
	}

	public void avancaCi() {
		this.CI += 4;
		if (this.CI >= (Modulo.memoria_ram.memoria.length / 2)){
			this.CI = 0;
		}
	}

	/**
	 * 
	 * Recebe sinal de endereço da RAM, após mandar um sinal de controle pedindo para ler
	 * 
	 * @param sinal_endereco
	 */
	public void recebeEndereco(int[] sinal_endereco) {
		this.endereco_atual = sinal_endereco[2];
	}

	/**
	 * Recebe sinal de dado da RAM
	 * 
	 * @param sinal_dado
	 */
	public void recebeDado(int[] sinal_dado) {
		if (sinal_dado.length == 2) { // É um valor
			this.valores.add(sinal_dado[1]);
		} else { // É uma instrução
			this.instrucao_atual = sinal_dado;
		}
		this.pode_processar_instrucao = true;
	}

}
