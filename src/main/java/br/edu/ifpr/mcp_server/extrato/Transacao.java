package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa um lançamento do extrato bancario (uma linha o CSV)
 * É um record: só guarda dados e não muda depois de criado
 */

public record Transacao(LocalDate data, BigDecimal valor, String descricao){

}
