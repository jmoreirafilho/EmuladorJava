package computer;

import main.Modulo;

public class Cpu implements Runnable {
	public int CI;
	
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
		this.CI++;
	}

}
