package computer;

import java.util.ArrayList;
import java.util.Arrays;

import com.sun.jmx.snmp.Timestamp;

import computer.Cache;
import main.Logs;
import main.Modulo;

public class Cpu implements Runnable {
	private Logs logger = new Logs();
	
	public int CI = 0;
	public int tamanho_da_cache = 0;
	private Cache[] memoria_cache;
	
	private boolean pode_processar_instrucao = false;

	private int endereco_atual;
	private ArrayList<Integer> instrucao_atual = new ArrayList<Integer>();
	private ArrayList<Integer> valores = new ArrayList<Integer>(); // tam max = 3
	
	// 0 => valor do label
	// 1 => indice do label
	private ArrayList<Integer> ci_provisorio = new ArrayList<Integer>(Arrays.asList(-1, -1, -1));

	private int registrador_a;
	private int registrador_b;
	private int registrador_c;
	private int registrador_d;

	private final int NUMERO_DESSE_MODULO = 3;
	private int resultado;
	private boolean pode_gravar_resultado;
	private boolean grava_sinal_controle_mov;
	private boolean pode_pedir_instrucao = true;
	private boolean pode_gravar_resposta_para_ram = false;
	private boolean pode_mandar_dado_mov = false;
	private int num_instrucoes_esperadas = 0;
	private boolean ultimo_loop = false;
	private boolean processamento_finalizado = false;
	private int tamanho_ocupado_cache = 0;
	
	// 1 -> FIFO
	// 2 -> LRU
	// 3 -> LFU
	private int politica_de_remocao;

	public Cpu(int tamanho_da_cache, int politica_de_remocao) {
		this.politica_de_remocao = politica_de_remocao;
		this.tamanho_da_cache = tamanho_da_cache;
		this.memoria_cache = new Cache[tamanho_da_cache];
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (this.pode_pedir_instrucao && Modulo.barramento.es_finalizada && !this.processamento_finalizado) {
				if (this.ultimo_loop) {
					this.processamento_finalizado  = true;
				}
				
				if (this.instrucao_atual.size() > 0 && !this.instrucaoEstaNaMemoriaCache()) {
					this.adicionaInstrucaoNaCache();
				} else {
					this.pedeInstrucaoParaRam();
				}
				
				this.avancaCi();
				this.pode_pedir_instrucao = false;
			}

			// Processa uma instrução
			if (this.pode_processar_instrucao) {
				this.processaInstrucao();
				this.pode_processar_instrucao = false;
			}
			
			if (this.pode_gravar_resultado) {
				this.pode_gravar_resultado = false;
				this.pode_gravar_resposta_para_ram  = true;
				this.gravaResultado();
			}
			
			if (this.grava_sinal_controle_mov) {
				System.out.println("CPU: gravou sinal de controle do MOV");
				logger.add("CPU: gravou sinal de controle do MOV");
				int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 1, this.instrucao_atual.get(3)};
				Modulo.barramento.fila_controle.add(sinal_controle);
				this.grava_sinal_controle_mov = false;
				this.pode_mandar_dado_mov = true;
			}

		}
	}

	private void adicionaInstrucaoNaCache() {
		int endereco = this.instrucao_atual.get(1);
		
		ArrayList<Integer> temp_instrucao_atual = this.instrucao_atual;
		
		while(!this.cacheTemEspaco(this.instrucao_atual.size())){
			this.removeElementoDaCache();
		}
		
		Timestamp ts = new Timestamp();
	
		for (int i = 0; i < this.tamanho_da_cache; i++) {
			if (this.memoria_cache[i] != null && this.memoria_cache[i].pegaIndiceDaRam() == -1) {
				this.memoria_cache[i].add(endereco, temp_instrucao_atual.get(0), ts.getDateTime());
				temp_instrucao_atual.remove(0);
				if (temp_instrucao_atual.size() <= 0) {
					break;
				}
			}
		}
	}

	/**
	 * @return True para caso haja a instrução do CI na cache, false caso contrário.
	 */
	private boolean instrucaoEstaNaMemoriaCache() {
		ArrayList<Integer> conteudo = new ArrayList<Integer>();
		
		// Percorre toda a cache
		for(int i = 0; i < this.tamanho_da_cache; i++) {
			// Verifica se o indice desse elemento é igual ao CI
			if (this.memoria_cache[i] != null && this.memoria_cache[i].pegaIndiceDaRam() != this.CI) {
				continue;
			}
			
			// Pega o conteudo do ArrayList nesse indice e joga em um vetor
			System.out.println("CPU: cache HIT de instrucao!");
			logger.add("CPU: cache HIT de instrucao!");
			conteudo.add(this.memoria_cache[i].conteudo);
		}
		
		if (conteudo.size() == 0) {
			System.out.println("CPU: cache MISS de instrucao!");
			logger.add("CPU: cache MISS de instrucao!");
			return false;
		}
		
		int[] sinal_dado = new int[conteudo.size()];
		for (int j = 0; j < sinal_dado.length; j++) {
			sinal_dado[j] = conteudo.get(j);
		}
		
		// Ao inves de mandar sinal de controle e esperar a instrução,
		// manda a instrução direto pro metodo de entrada de dados da CPU.
		this.recebeDado(sinal_dado);
		return true;
	}

	/**
	 * Manda sinal de controle para memoria Ram, pedindo nova instrução.
	 */
	private void pedeInstrucaoParaRam() {
		System.out.println("CPU: pediu instrução na posição "+this.CI);
		logger.add("CPU: pediu instrução na posição "+this.CI);
		int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 0, this.CI};
		Modulo.barramento.fila_controle.add(sinal_controle);
	}

	private void gravaResultado() {
		// Grava o resultado na Cache antes
		
		if (this.instrucao_atual.get(3) < -5) { // posição na memória ram
			System.out.println("CPU: mandou sinal de controle para gravar resultado na RAM");
			logger.add("CPU: mandou sinal de controle para gravar resultado na RAM");
			int posicao_real = (this.instrucao_atual.get(3) * -1) - 6 + (Modulo.memoria_ram.tamanho / 2);
			int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 1, posicao_real};
			Modulo.barramento.fila_controle.add(sinal_controle);
		} else if (this.instrucao_atual.get(3) > -6 && this.instrucao_atual.get(3) < -1) { // registrador
			System.out.println("CPU: gravou resultado nos registradores");
			logger.add("CPU: gravou resultado nos registradores");
			switch (this.instrucao_atual.get(3)) {
			case -2: // A
				this.defineRegistradorA(this.resultado);
				break;
			case -3: // B
				this.defineRegistradorB(this.resultado);
				break;
			case -4: // C
				this.defineRegistradorC(this.resultado);
				break;
			case -5: // D
				this.defineRegistradorD(this.resultado);
				break;
			}
			System.out.print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n| "+this.registrador_a+" | "+this.registrador_b+" | "+this.registrador_c+" | "+this.registrador_d+" |\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
			this.preparaCpuParaProximaInstrucao();
		}
	}

	private void processaInstrucao() {
		switch (this.instrucao_atual.get(2)) {
		case 1: // ADD
			System.out.println("CPU: processou instrucao ADD");
			logger.add("CPU: processou instrucao ADD");
			this.processaAdd();
			break;
		case 2: // MOV
			System.out.println("CPU: processou instrucao MOV");
			logger.add("CPU: processou instrucao MOV");
			this.processaMov();
			break;
		case 3: // IMUL
			this.processaImul();
			System.out.println("CPU: processou instrucao IMUL");
			logger.add("CPU: processou instrucao IMUL");
			break;
		case 4: // INC
			this.processaInc();
			System.out.println("CPU: processou instrucao INC");
			logger.add("CPU: processou instrucao INC");
			break;
		case 5: // DEC
			this.processaDec();
			System.out.println("CPU: processou instrucao DEC");
			logger.add("CPU: processou instrucao DEC");
			break;
		case 6: // LABEL
			this.processaLabel();
			System.out.println("CPU: processou instrucao LABEL");
			logger.add("CPU: processou instrucao LABEL");
			break;
		case 7: // JUMP
			System.out.println("CPU: processou instrucao JUMP");
			logger.add("CPU: processou instrucao JUMP");
			this.processaJump();
			break;
		}
	}

	private void processaAdd() {
		int valor1 = this.valores.get(0);
		int valor2 = this.valores.get(1);
		this.resultado = valor1 + valor2;
		this.pode_gravar_resultado = true;
	}

	private void processaMov() {
		if (this.instrucao_atual.get(3) < -5) { // posicao da memória ram
			this.resultado = this.valores.get(1);
			this.grava_sinal_controle_mov = true;
			System.out.println("CPU: processou mov: "+this.resultado);
			logger.add("CPU: processou mov: "+this.resultado);
		} else { // registrador
			switch (this.instrucao_atual.get(3)) {
			case -2: // A
				this.defineRegistradorA(this.valores.get(1));
				break;
			case -3: // B
				this.defineRegistradorB(this.valores.get(1));
				break;
			case -4: // C
				this.defineRegistradorC(this.valores.get(1));
				break;
			case -5: // D
				this.defineRegistradorD(this.valores.get(1));
				break;
			}
		}
	}

	private void processaImul() {
		int valor1 = this.valores.get(1);
		int valor2 = this.valores.get(2);
		this.resultado = valor1 * valor2;
		this.pode_gravar_resultado = true;
	}

	private void processaInc() {
		int valor = this.valores.get(0);
		this.resultado = valor + 1;
		this.pode_gravar_resultado = true;
	}
	
	private void processaDec() {
		int valor = this.valores.get(0);
		this.resultado = valor - 1;
		this.pode_gravar_resultado = true;
	}
	
	private void processaLabel() {
		this.ci_provisorio.set(0, this.valores.get(0));
		this.ci_provisorio.set(1, this.CI);
		this.preparaCpuParaProximaInstrucao();
	}
	
	private void processaJump() {
		int param_1 = this.valores.get(0);
		int simbolo_comparativo = this.valores.get(1);
		int param_2 = this.valores.get(2);
		int indice_do_jump = this.instrucao_atual.get(6);
		
		boolean condicao = false;
		
		switch (simbolo_comparativo) {
		case -2: // >
			if (param_1 > param_2) {
				condicao = true;
			}
			break;
		case -3: // <
			if (param_1 < param_2) {
				condicao = true;
			}
			break;
		case -4: // >=
			if (param_1 >= param_2) {
				condicao = true;
			}
			break;
		case -5: // <=
			if (param_1 <= param_2) {
				condicao = true;
			}
			break;
		case -6: // ==
			if (param_1 == param_2) {
				condicao = true;
			}
			break;
		case -7: // !=
			if (param_1 != param_2) {
				condicao = true;
			}
			break;
		}
		
		if (condicao && this.ci_provisorio.get(0) == indice_do_jump) {
			System.out.println("CPU: condição para loop satisfeita, pula pro indice: "+this.ci_provisorio.get(1));
			logger.add("CPU: condição para loop satisfeita, pula pro indice: "+this.ci_provisorio.get(1));
			this.CI = this.ci_provisorio.get(1);
			this.processamento_finalizado = false;
			this.ultimo_loop = false;
		} else {
			System.out.println("CPU: condição para o loop não satisfeita. Sai do loop.");
			logger.add("CPU: condição para o loop não satisfeita. Sai do loop.");
			this.CI++;

			if (this.CI >= Modulo.barramento.ultima_posicao_inserida) {
				this.ultimo_loop = true;
			}
		}
		
		this.preparaCpuParaProximaInstrucao();
	}

	public int pegaRegistradorA() {
		return registrador_a;
	}

	public void defineRegistradorA(int registrador_a) {
		System.out.println("CPU: move "+registrador_a+" para o registrador A");
		this.registrador_a = registrador_a;
	}

	public int pegaRegistradorB() {
		return registrador_b;
	}

	public void defineRegistradorB(int registrador_b) {
		System.out.println("CPU: move "+registrador_b+" para o registrador B");
		this.registrador_b = registrador_b;
	}

	public int pegaRegistradorC() {
		return registrador_c;
	}

	public void defineRegistradorC(int registrador_c) {
		System.out.println("CPU: move "+registrador_c+" para o registrador C");
		this.registrador_c = registrador_c;
	}

	public int pegaRegistradorD() {
		return registrador_d;
	}

	public void defineRegistradorD(int registrador_d) {
		System.out.println("CPU: move "+registrador_d+" para o registrador D");
		this.registrador_d = registrador_d;
	}

	public void avancaCi() {
		if (this.ultimo_loop) {
			System.out.println("CPU: fim do Processamento");
			Thread.interrupted();
			this.pode_pedir_instrucao = false;
		}
		this.CI += 4;
		if (this.CI >= (Modulo.memoria_ram.memoria.length / 2)) {
			this.CI = 0;
		}
	}

	/**
	 * 
	 * Recebe sinal de endereço da RAM, após mandar um sinal de controle pedindo
	 * para ler
	 * 
	 * @param sinal_endereco
	 */
	public void recebeEndereco(int[] sinal_endereco) {
		System.out.println("CPU: recebeu sinal de endereco (Orig: "+sinal_endereco[0]+", Dest: "+sinal_endereco[1]+", Ende: "+sinal_endereco[2]+")");
		logger.add("CPU: recebeu sinal de endereco (Orig: "+sinal_endereco[0]+", Dest: "+sinal_endereco[1]+", Ende: "+sinal_endereco[2]+")");
		this.endereco_atual = sinal_endereco[2];
		if (this.pode_mandar_dado_mov) {
			this.mandaSinalDadoMov();
			this.preparaCpuParaProximaInstrucao();
		} else if (this.pode_gravar_resposta_para_ram) {
			System.out.println("CPU: mandou sinal de dado");
			logger.add("CPU: mandou sinal de dado");
			int[] sinal_dado = {2, this.endereco_atual, this.resultado};
			Modulo.barramento.fila_dado.add(sinal_dado);
			this.preparaCpuParaProximaInstrucao();
		}
	}

	private void preparaCpuParaProximaInstrucao() {
		this.instrucao_atual.clear();
		this.valores.clear();
		this.pode_pedir_instrucao = true;
		this.pode_gravar_resposta_para_ram = false;
		this.num_instrucoes_esperadas = 0;
		System.out.println("CPU: resetou valores para proximo loop");
		logger.add("CPU: resetou valores para proximo loop");
	}

	private void mandaSinalDadoMov() {
		int posicao_real = (this.endereco_atual * -1) - 6 + (Modulo.memoria_ram.tamanho / 2);
		int[] sinal_dado = {2, posicao_real, this.resultado};
		Modulo.barramento.fila_dado.add(sinal_dado);
		this.pode_mandar_dado_mov = false;
		System.out.println("CPU: gravou sinal de dado (MOV)");
		logger.add("CPU: gravou sinal de dado (MOV)");
	}

	/**
	 * Recebe sinal de dado da RAM
	 * 
	 * @param sinal_dado
	 */
	public void recebeDado(int[] sinal_dado) {
		if (sinal_dado.length == 3) { // É um valor
			System.out.println("CPU: recebeu sinal de dado -> VALOR: "+sinal_dado[2]);
			logger.add("CPU: recebeu sinal de dado -> VALOR: "+sinal_dado[2]);
			this.adicionaNaPrimeiraPosicaoDeValoresDisponivel(sinal_dado[1], sinal_dado[2]);
			this.num_instrucoes_esperadas--;
		} else { // É uma instrução
			System.out.print("CPU: recebeu sinal de dado -> Instrucao: ");
			logger.add("CPU: recebeu sinal de dado -> Instrucao: ");
			for (int i = 0; i < sinal_dado.length; i++) {
				System.out.print(sinal_dado[i] + " | ");
				logger.add(sinal_dado[i] + " | ");
				this.instrucao_atual.add(sinal_dado[i]);
			}
			System.out.print("\n");
			this.pegaValoresDosParametros();
		}
		this.verificaSeInstrucaoEstaProntaParaSerProcessada();
	}

	private void verificaSeInstrucaoEstaProntaParaSerProcessada() {
		System.out.println("CPU: esperando por "+this.num_instrucoes_esperadas+" parametros");
		logger.add("CPU: esperando por "+this.num_instrucoes_esperadas+" parametros");
		for (int i = 0; i < this.valores.size(); i++) {
			if (i == 0) {
				System.out.println("---------------------------------------------------------------------------");
			}
			System.out.print("| "+this.valores.get(i)+" | ");
		}
		System.out.println("");
		
		if (this.instrucao_atual.size() > 0 && this.instrucao_atual.get(2) > 0 
				&& this.instrucao_atual.get(2) < 8 && this.valores.size() == 3 
				&& this.num_instrucoes_esperadas == 0) {
			this.pode_processar_instrucao = true;
		}
	}

	private void adicionaNaPrimeiraPosicaoDeValoresDisponivel(int endereco, int valor) {
		System.out.println("CPU: recebeu um valor para um parametro buscado");
		logger.add("CPU: recebeu um valor para um parametro buscado");
		boolean pode_adicionar = true;
		for (int i = 0; i < this.valores.size(); i++) {
			if (this.valores.size() > 0 && this.valores.get(i) == -1 && pode_adicionar) {
				pode_adicionar = false;
				
				// Adiciona valor recebido na cache
				this.verificaParaAdicionarNaCache(endereco, valor);
				
				// adiciona o valor
				System.out.println("CPU: adicionou "+valor+" na posicao "+i);
				this.valores.set(i, valor);
			}
		}
	}

	private void pegaValoresDosParametros() {
		if (this.valores.size() == 0) {
			for (int i = 0; i < 3; i++) {
				this.valores.add(-1);
			}	
		}
		
		// é um mov ou imul, nao precisa pegar o primeiro valor, apenas converter
		if (this.instrucao_atual.get(2) > 1 && this.instrucao_atual.get(2) < 4) { 
			int posicao_real = ( this.instrucao_atual.get(3) * -1 ) - 6 + (Modulo.memoria_ram.tamanho / 2);
			System.out.println("CPU: Converte "+this.instrucao_atual.get(3)+" para "+posicao_real);
			logger.add("CPU: Converte "+this.instrucao_atual.get(3)+" para "+posicao_real);
			this.valores.set(0, posicao_real);
		} else {
			// Primeiro indice, referente ao primeiro valor do parametro
			this.pedeValorParaRam(0, this.instrucao_atual.get(3));
		}
		
		if (this.instrucao_atual.get(4) != -1) {
			// Segundo indice, referente ao segundo valor do parametro
			this.pedeValorParaRam(1, this.instrucao_atual.get(4));
		}
		
		if (this.instrucao_atual.get(5) != -1) {
			// Terceiro indice, referente ao terceiro valor do parametro
			this.pedeValorParaRam(2, this.instrucao_atual.get(5));
		}
	}

	private void pedeValorParaRam(int indice, int parametro) {
		// Verifica se o valor ja esta na cache
			// Se for uma instruçao de loop, grava logo o valor do parametro
			if (this.instrucao_atual.get(2) == 7 && indice == 1) {
				this.valores.set(1, parametro);
			} else {
				System.out.println("CPU: computa parametro "+parametro);
				logger.add("CPU: computa parametro "+parametro);
				if (parametro > -6 && parametro < -1) {
					// registrador
					switch (parametro) {
					case -2:
						this.valores.set(indice, this.pegaRegistradorA());
						break;
					case -3:
						this.valores.set(indice, this.pegaRegistradorB());
						break;
					case -4:
						this.valores.set(indice, this.pegaRegistradorC());
						break;
					case -5:
						this.valores.set(indice, this.pegaRegistradorD());
						break;
					}
				} else if (parametro < -5) {
					// posição de memória
					int parametro_real = (parametro * -1) - 6 + (Modulo.memoria_ram.tamanho / 2);
					this.instrucaoEstaNaCache(indice, parametro_real);
				} else {
					this.valores.set(indice, parametro);
				}
			}
	}

	private void instrucaoEstaNaCache(int indice, int parametro_real) {
		for(int i = 0; i < this.tamanho_da_cache; i++) {
			if (this.memoria_cache[i] != null) {
				if (i == 0) {
					System.out.println("\n");
				}
				
				System.out.print(this.memoria_cache[i].pegaIndiceDaRam()+" -- ");
				
				if (i == (this.tamanho_da_cache - 1)) {
					System.out.println("\n");
				}
			}
		}
		
		boolean hit = false, pode_dar_miss = false;
		
		for(int i = 0; i < this.tamanho_da_cache; i++) {
			if (this.memoria_cache[i] != null) {
				pode_dar_miss = true;
				if(this.memoria_cache[i].pegaIndiceDaRam() == parametro_real) {
					this.valores.set(indice, this.memoria_cache[i].conteudo);
					System.out.println("CPU: cache HIT para valor na posicao "+parametro_real);
					logger.add("CPU: cache HIT para valor na posicao "+parametro_real);
					hit = true;
				}
			} else {
				pode_dar_miss = false;
			}
		}
		
		if (!hit) {
			if(pode_dar_miss){
				System.out.println("CPU: cache MISS para valor na posicao "+parametro_real);
				logger.add("CPU: cache MISS para valor na posicao "+parametro_real);
			} else {
				System.out.println("CPU: não deu cache MISS pois ainda há espaço na cache");
				logger.add("CPU: não deu cache MISS pois ainda há espaço na cache");
			}
			System.out.println("CPU: manda sinal de controle para buscar parametro na posição "+parametro_real);
			logger.add("CPU: manda sinal de controle para buscar parametro na posição "+parametro_real);
			int[] sinal_de_controle = {NUMERO_DESSE_MODULO, 2, 0, parametro_real};
			Modulo.barramento.fila_controle.add(sinal_de_controle);
		}
		this.num_instrucoes_esperadas++;
	}

	/**
	 * Procedimento para adicionar valor unico na cache.
	 * 
	 * @param endereco
	 * @param valor
	 */
	private void adicionaValorNaCache(int endereco, int valor) {
		while (!this.cacheTemEspaco(1)) {
			this.removeElementoDaCache();
		}
		
		for (int i = 0; i < this.tamanho_da_cache; i++) {
			if (this.memoria_cache[i] != null && this.memoria_cache[i].pegaIndiceDaRam() == -1) {
				this.memoria_cache[i].add(endereco, valor, null);
			}
		}
	}

	/**
	 * Método para remover da cache algum elemento, baseado na politica de remoção.
	 * Esse elemento pode ser uma instrução inteira ou apenas um unico elemento.
	 */
	private void removeElementoDaCache() {
		// Caso a politica seja FIFO
		if (this.politica_de_remocao == 1) {
			int indice_da_ram_para_ser_removido = 0, indice_para_ser_removido = 0;

			// Percorre o elemento mais antigo e pega o indiceDaRam dele
			for(int i = 0; i < this.tamanho_da_cache; i++) {
				Long temp = this.memoria_cache[i].pegaTimestamp();
				if (temp < this.memoria_cache[indice_para_ser_removido].pegaTimestamp()) {
					indice_para_ser_removido = i;
					indice_da_ram_para_ser_removido = this.memoria_cache[indice_para_ser_removido].pegaIndiceDaRam();
				}
			}
			
			// Remove o(s) elemento(s) mais antigo(os)
			for (int i = 0; i < this.tamanho_da_cache; i++) {
				if (this.memoria_cache[i].pegaIndiceDaRam() == indice_da_ram_para_ser_removido) {
					this.memoria_cache[i].remove();
					this.tamanho_ocupado_cache -= 1;
				}
			}
		}
		
		// Caso a politica seja LRU
		if (this.politica_de_remocao == 2) {
			
		}
		
		// Caso a politica seja LFU
		if (this.politica_de_remocao == 3) {
			
		}
	}

	/**
	 * Adiciona valor unico na cache.
	 * 
	 * @param conteudo
	 */
	private void verificaParaAdicionarNaCache(int endereco, int conteudo) {
		boolean deve_adicionar = true;
		
		// Verifica se ja esta na cache
		for(int i = 0; i < this.tamanho_da_cache; i++){
			if (this.memoria_cache[i] != null && this.memoria_cache[i].pegaConteudo() == conteudo) {
				this.memoria_cache[i].renova();
				deve_adicionar = false;
				break;
			}
		}
		
		// Caso não esteja na cache, adiciona
		if (deve_adicionar) {
			this.adicionaValorNaCache(endereco, conteudo);
		}
		
//		this.memoria_cache[this.tamanho_ocupado_cache].add(endereco, conteudo, null);
	}

	/**
	 * @param espaco_necessario
	 * @return True caso haja espaço na memoria cache, False caso contrário.
	 */
	private boolean cacheTemEspaco(int espaco_necessario) {
		int espaco_real = this.tamanho_da_cache - this.tamanho_ocupado_cache;
		if (espaco_necessario <= espaco_real) {
			return true;
		}
		return false;
	}

}