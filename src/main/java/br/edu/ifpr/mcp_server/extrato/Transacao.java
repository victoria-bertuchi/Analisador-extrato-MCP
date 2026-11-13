package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transacao(LocalDate data, BigDecimal valor, String descricao){

}
