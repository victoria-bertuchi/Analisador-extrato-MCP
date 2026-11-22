package br.edu.ifpr.mcp_server.extrato;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Contrato de todo leitor de extrato: recebe o arquivo e devolve a lista de transações.
 * Cada formato de banco tem o seu leitor (padrão Strategy), e quem escolhe qual usar
 * é a FabricaDeLeitor (padrão Factory).
 */
public interface LeitorDeExtrato {
    List<Transacao> ler(Path arquivo) throws IOException;
}