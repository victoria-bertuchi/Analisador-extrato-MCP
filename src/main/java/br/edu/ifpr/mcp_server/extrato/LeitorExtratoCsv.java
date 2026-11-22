package br.edu.ifpr.mcp_server.extrato;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Lê o extrato exportado do banco (CSV no formato date, title, amount)
 * transforma cada linha em uma transação 
 */

public class LeitorExtratoCsv {
	/**
	 * Lê o arquivo, pula o cabelçalho e linhas vazias
	 * @param arquivo caminho do CSV
	 * @return lista de transações do extrato
	 * @throws IOException se o arquivo não existir ou não puder ser lido
	 */
	
	public List<Transacao> ler (Path arquivo) throws IOException{
		return Files.readAllLines(arquivo)
					.stream()
					.skip(1)
					.filter(linha -> !linha.isBlank())
					.map(this::converterLinha)
					.toList();
	}
	
	/**
	 * Converte uma linha do CSV em transação 
	 */
	
	private Transacao converterLinha(String linha) {
		String[] partes = linha.split(",");
		LocalDate data = LocalDate.parse(partes[0].trim());
		String descricao = partes[1].trim();
		BigDecimal valor = new BigDecimal(partes[2].trim());
		return new Transacao(data, valor, descricao);
	}

}
