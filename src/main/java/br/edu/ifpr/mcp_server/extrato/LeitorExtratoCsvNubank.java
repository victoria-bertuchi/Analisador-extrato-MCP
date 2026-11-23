package br.edu.ifpr.mcp_server.extrato;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Lê o CSV do Nubank (date,title,amount).
 * Nesse formato o gasto já vem positivo, ao contrário do extrato de conta.
 */
public class LeitorExtratoCsvNubank implements LeitorDeExtrato {

    // reaproveita o leitor genérico, que já trata aspas, vírgula dentro da descrição e datas
    private final LeitorDeExtrato leitorGenerico = new LeitorExtratoCsvGenerico();

    @Override
    public List<Transacao> ler(Path arquivo) throws IOException {
        // o genérico inverte o sinal (pensando em extrato de conta); aqui desfazemos a inversão
        return leitorGenerico.ler(arquivo).stream()
                .map(t -> new Transacao(t.data(), t.valor().negate(), t.descricao()))
                .toList();
    }
}