package computer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Modulo;

/**
 * @author Airton
 *
 * Dicionario -------
 *  1 -> ADD   || ES
 *  2 -> MOV   || RAM
 *  3 -> IMUL  || CPU
 *  4 -> INC
 * -1 -> null
 * -2 -> A
 * -3 -> B
 * -4 -> C
 * -5 -> D
 * -6 -> Daqui pra menos ficam as posições de memória [ex: 0x004 = 4 => ((4 + 6) * -1) => -10]
 *
 */
public class EntradaSaida implements Runnable {
	public Thread thread;
	public ArrayList<String> instrucoes_informadas = new ArrayList<String>();
	public ArrayList<int[]> instrucoes_convertidas = new ArrayList<int[]>();
	private BufferedReader leitor;
	
	
	/**
	 * Ler o arquivo e preenche a lista de instruções.
	 * Chama o método para validar e converter as instruções
	 */
	public boolean compilaArquivo() {
		// Lê o arquivo e passa para a lista de instruções
		String caminho = "C:\\Users\\Airton\\workspace\\Emulador\\src\\main\\asm.txt";
		try {
			leitor = new BufferedReader(new FileReader(caminho));
			int linha = 1;
			while (leitor.ready()) {
				// Valida a sintaxe de cada instrução
				if(!this.analisadorSintatico(leitor.readLine().toLowerCase())){
//					System.out.println("Ocorreu um erro na linha "+ linha);
					return false;
				}
				linha++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		int instrucao_atual = 0;
		
		int[] sinal_controle = {1, 2};
		
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int posicao_controle = Modulo.barramento.contador_escrita_controle;
			if(this.instrucoes_convertidas.size() > posicao_controle && posicao_controle == instrucao_atual) {
				Modulo.barramento.adicionaFilaControle(posicao_controle, sinal_controle);
//				System.out.println("ES: gravou controle");
			}
			
			if (Modulo.barramento.fila_endereco.size() > instrucao_atual && Modulo.barramento.fila_endereco.get(instrucao_atual).length > 0 && Modulo.barramento.fila_endereco.get(instrucao_atual)[0] == 1) {
				int[] instrucao = this.buffer(Modulo.cpu.CI);
				if(instrucao == null){
//					System.out.println("Acabaram os comandos!");
					break;
				}
				
				instrucao[0] = 1;

//				System.out.println("ES: gravou dado");
				Modulo.barramento.adicionaFilaDado(instrucao_atual, instrucao);
				instrucao_atual++;
			}
		}
	}
	
	public int[] buffer(int contador_instrucao) {
		if(contador_instrucao > instrucoes_convertidas.size()){
			return null;
		}
		return instrucoes_convertidas.get(contador_instrucao);
	}
	
	public boolean analisadorSintatico(String comando) {
		Matcher acerto_add = Pattern.compile("^add\\s+(\\w+)\\s*,\\s*(\\w+)\\s*$").matcher(comando);
		Matcher acerto_imul = Pattern.compile("^imul\\s+(\\w+)\\s*,\\s*(\\w+)\\s*,\\s*(\\w+)\\s*$").matcher(comando);
		Matcher acerto_mov = Pattern.compile("^mov\\s+(\\w+)\\s*,\\s*(\\w+)\\s*$").matcher(comando);
		Matcher acerto_inc = Pattern.compile("^inc\\s+(\\w+)\\s*$").matcher(comando);

		int[] comando_convertido = null;
		
		if (acerto_add.matches()) {
			comando_convertido = this.converteComandoAdd(acerto_add);
		} else if (acerto_imul.matches()) {
			comando_convertido = this.converteComandoImul(acerto_imul);
		} else if (acerto_mov.matches()) {
			comando_convertido = this.converteComandoMov(acerto_mov);
		} else if (acerto_inc.matches()) {
			comando_convertido = this.converteComandoInc(acerto_inc);
		}
		
		if(comando_convertido != null){
			instrucoes_convertidas.add(comando_convertido);
			return true;
		}
		return false;
	}

	public Integer converteValor(String valor) {
		if(valor.length() > 2 && (valor.substring(0, 2)).equals("0x")){
//			System.out.println(Modulo.memoria_ram.tamanho);
			if(Long.parseLong(valor.replace("0x", ""), 16) > Modulo.memoria_ram.tamanho){
//				System.out.println("Valor informador maior que o parametrizado!");
				return null;
			}
			// +6 = pula para o primeiro valor depois do 5, para quando multiplicar por -1
			return ((Integer.parseInt(valor.replace("0x", "")) + 6) * -1);
		} else if (valor.equals("a") || valor.equals("b") || valor.equals("c") || valor.equals("d")) {
			switch (valor) {
			case "a":
				return -2;
			case "b":
				return -3;
			case "c":
				return -4;
			case "d":
				return -5;
			}
		} else if (Integer.parseInt(valor) >= 0) {
			return Integer.parseInt(valor);
		}
		return null;
	}
	
	private int[] converteComandoAdd(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		Integer valor2 = this.converteValor(acerto.group(2));
		if(valor1 == null || valor2 == null){
			return null;
		}
		int[] valores = {-1, 1, valor1, valor2, -1};
		return valores;
	}
	
	private int[] converteComandoMov(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		Integer valor2 = this.converteValor(acerto.group(2));
		if(valor1 == null || valor2 == null){
			return null;
		}
		int[] valores = {-1, 2, valor1, valor2, -1};
		return valores;
	}
	
	private int[] converteComandoImul(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		Integer valor2 = this.converteValor(acerto.group(2));
		Integer valor3 = this.converteValor(acerto.group(3));
		if(valor1 == null || valor2 == null || valor3 == null){
			return null;
		}
		int[] valores = {-1, 3, valor1, valor2, valor3};
		return valores;
	}
	
	private int[] converteComandoInc(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		if(valor1 == null){
			return null;
		}
		int[] valores = {-1, 4, valor1, -1, -1};
		return valores;
	}
	
	
}