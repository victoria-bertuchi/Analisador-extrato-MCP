package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.ia.ClienteIA;

/**
 * Testes da classificação por IA, usando uma IA falsa no lugar do Gemini.
 * Assim os testes rodam sem internet, sem chave e sempre com a mesma resposta.
 */

public class ClassificadorPorIATest {
	//é utilizado uma IA falsa. assina o mesmo contrato do ClienteGemini, mas não acessa a internet.
    //Ela só devolve a resposta que recebeu no construtor.
    private static class IaFalsa implements ClienteIA {
        private final String resposta;

        IaFalsa(String resposta) {
            this.resposta = resposta;
        }

        @Override
        public String perguntar(String instrucoesDoSistema, String mensagemDoUsuario) {
            return resposta;
        }
    }

    //IA falsa que sempre dá erro, para imitar a falta de internet ou o servidor fora do ar
    private static class IaComErro implements ClienteIA {
        @Override
        public String perguntar(String instrucoesDoSistema, String mensagemDoUsuario) throws Exception {
            throw new Exception("Sem conexão");
        }
    }

    //data e valor são fixos porque o classificador só usa a descrição
    private Transacao transacao(String descricao) {
        return new Transacao(LocalDate.now(), BigDecimal.TEN, descricao);
    }

    //conferir que, quando a IA responde o nome de uma categoria,
    //o classificar transforma esse texto na categoria certa
    @Test
    @DisplayName("Resposta da IA vira a categoria correspondente")
    void respostaViraCategoria() {
        ClassificadorPorIA classificador = new ClassificadorPorIA(new IaFalsa("DELIVERY"));
        Categoria resultado = classificador.classificar(transacao("Ifd*Saiko Sushi Comerc"));
        assertEquals(Categoria.DELIVERY, resultado);
    }

    //conferir que espaços e letras minúsculas na resposta da IA não atrapalham,
    //porque o classificar usa trim (tira espaços) e toUpperCase (passa para maiúsculo)
    @Test
    @DisplayName("Resposta com espaços e minúsculas também funciona")
    void respostaComEspacosEMinusculas() {
        ClassificadorPorIA classificador = new ClassificadorPorIA(new IaFalsa("  transporte \n"));
        Categoria resultado = classificador.classificar(transacao("Uber *Trip"));
        assertEquals(Categoria.TRANSPORTE, resultado);
    }

    //conferir que, se a IA responder algo que não é uma categoria,
    //classificar não quebra e devolve OUTROS
    @Test
    @DisplayName("Resposta que não é categoria vira OUTROS")
    void respostaInvalidaViraOutros() {
        ClassificadorPorIA classificador = new ClassificadorPorIA(new IaFalsa("Acho que é comida"));
        Categoria resultado = classificador.classificar(transacao("Loja Qualquer"));
        assertEquals(Categoria.OUTROS, resultado);
    }

    // conferir que, se não der para falar com a IA, o classificar avisa com um erro claro
    //(assertThrows passa quando o código lança o erro esperado)
    @Test
    @DisplayName("Falha na comunicação com a IA gera erro")
    void falhaDaIaGeraErro() {
        ClassificadorPorIA classificador = new ClassificadorPorIA(new IaComErro());
        assertThrows(IllegalStateException.class,
                () -> classificador.classificar(transacao("Uber *Trip")));
    }

}
