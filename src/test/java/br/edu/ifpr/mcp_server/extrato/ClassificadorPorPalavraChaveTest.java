package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes automáticos da classificação por palavra-chave.
 * Cada teste segue 3 passos: prepara uma transação, chama o método classificar
 * e confere com assertEquals se a categoria devolvida é a esperada.
 */

public class ClassificadorPorPalavraChaveTest {
	//cria o classificador q vai ser testado, passando uma tabela pequena de palavras
	//DELIVERY -> "ifd" e TRANSPORTE -> "uber"(Mpa .of monta a tabela, List.of monta a lista de palavras)
	
	private final ClassificadorPorPalavraChave classificador = new ClassificadorPorPalavraChave(
            Map.of(Categoria.DELIVERY, List.of("ifd"),
                   Categoria.TRANSPORTE, List.of("uber")));
	//data e valor são fixos porque o classificador só olha a descrição
    private Transacao transacao(String descricao) {
        return new Transacao(LocalDate.now(), BigDecimal.TEN, descricao);
    }
    
    //conferir que, quando a descrição tem uma palavra da tabela ("uber"),
    //o classificar devolve a categoria ligada a essa palavra (TRANSPORTE)
    @Test
    @DisplayName("Descrição com palavra-chave recebe a categoria certa")
    void classificaPelaPalavraChave() {
        Transacao compra = transacao("Uber *Trip");
        Categoria resultado = classificador.classificar(compra);
        assertEquals(Categoria.TRANSPORTE, resultado);
    }
    
    //conferir que a descrição em MAIÚSCULO, como vem no extrato,
    // ainda é reconhecida, porque o classificar converte para minúsculo antes de comparar com a tabela
    @Test
    @DisplayName("Maiúsculas e minúsculas não fazem diferença")
    void ignoraMaiusculas() {
        Transacao compra = transacao("IFD*SAIKO SUSHI COMERC");
        Categoria resultado = classificador.classificar(compra);
        assertEquals(Categoria.DELIVERY, resultado);
    }

    //conferir que, quando a descrição não tem nenhuma palavra da tabela,
    //o classificar não inventa uma categoria e devolve OUTROS
    @Test
    @DisplayName("Descrição sem nenhuma palavra-chave vira OUTROS")
    void semPalavraChaveViraOutros() {
        Transacao compra = transacao("Loja Qualquer");
        Categoria resultado = classificador.classificar(compra);
        assertEquals(Categoria.OUTROS, resultado);
    }
    
    


}
