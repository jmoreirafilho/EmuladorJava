package computer;

import main.Modulo;

public class Cpu implements Runnable {
	public int CI = 0;
	private boolean pode_pedir_instrucao_da_ram = true;
	private boolean pode_mandar_sinal_dado = false;
	private boolean pode_processar_dado = false;
	
	private int endereco_atual;
	
	private int[] dado_atual;
	
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
			
			if (this.pode_pedir_instrucao_da_ram) {
				// Manda sinal de controle para ler uma instrução da memoria RAM
				int[] sinal_controle_leitura = {NUMERO_DESSE_MODULO, 2, 0, this.CI};
				this.avancaCi();
				
				// Manda sinal de controle, pedindo instrução da RAM
				Modulo.barramento.adicionaFilaControle(sinal_controle_leitura);
				
				// Define para não pedir outra instrução enquanto a atual nao tiver
				// sido processada
				this.pode_pedir_instrucao_da_ram = false;
			}
			
			if (this.pode_processar_dado) {

				if (this.precisaPegarDadoNaRam()) {
					// Manda sinal de leitura para pegar dado da ram
					this.processaInstrucaoDependente();
				} else { // Processa e termina
					this.processaInstrucaoIndependente();	
				}
				
				// Grava resultado na RAM
				if (this.dado_atual[3] < -5) {
					this.gravaResultadoNaRam();
				} else if(this.dado_atual[3] < -1 && this.dado_atual[3] > -6) {
					this.gravaResultadoNoRegistrador();
				} else {
					this.dado_foi_processado = true;
				}

				if (this.dado_foi_processado) {
					// Define para pedir outra instrução da RAM no próximo loop
					this.pode_pedir_instrucao_da_ram = true;	
				}
			}
			
		}
	}

	/**
	 * Verifica se o dado_atual precisa de algum valor que esteja na RAM
	 * 
	 * @return
	 */
	private boolean precisaPegarDadoNaRam() {
		if (this.dado_atual[3] < -5 || this.dado_atual[4] < -5 || this.dado_atual[5] < -5) {
			return true;
		}
		return false;
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
		this.dado_atual = sinal_dado;
		this.pode_processar_dado = true;
	}

}
