package br.edu.ifpr.mcp_server.extrato;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class LeitorExtratoCsv {
	
	public List<Transacao> ler (Path arquivo) throws IOException{
		return Files.readAllLines(arquivo)
					.stream()
					.skip()
					.filter(linha -> !linha.isBlank())
					.map(this::converterLinha)
					.toList();
	}
	
	private Transacao converterLinha(String linha) {
		String[] partes = linha.split(",");
		LocalDate data = LocalDate.parse(partes[0].trim());
		String descricao = partes[1].trim();
		BigDecimal valor = new BigDecimal(partes[2].trim());
	}

}
