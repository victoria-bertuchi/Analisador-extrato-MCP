package br.edu.ifpr.mcp_server.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/**
 * Teste de integração: sobe o servidor MCP (o .jar gerado pelo build) e executa a ferramenta
 * gastos_por_categoria como um cliente MCP faria.
 */
class GastosPorCategoriaToolIT {

    private static final String SERVER_PATH = "server/mcp_server01_server.jar";
    private static final String NOME_FERRAMENTA = "gastos_por_categoria";
    private static final String CSV_EXEMPLO = "src/test/resources/extratos/fatura_nubank.csv";
    private static final String CHAVE = System.getenv("GEMINI_API_KEY");

    private static McpSyncClient client;

    @BeforeAll
    static void iniciarServidor() {
        File serverJar = new File(SERVER_PATH);
        assertTrue(serverJar.isFile(), "JAR do servidor não encontrado: " + serverJar.getAbsolutePath());

        ServerParameters.Builder parametros = ServerParameters.builder("java")
                                                              .args("-jar", serverJar.getAbsolutePath());
        // Repassa a chave da IA para o servidor, se ela existir no computador que roda o teste
        if (CHAVE != null && !CHAVE.isBlank()) {
            parametros = parametros.addEnvVar("GEMINI_API_KEY", CHAVE);
        }

        StdioClientTransport transporte = new StdioClientTransport(parametros.build(),
                                                                   new JacksonMcpJsonMapper(new JsonMapper()));
        client = McpClient.sync(transporte).build();
        client.initialize();
    }

    @AfterAll
    static void encerrarServidor() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    //chama a ferramenta com os argumentos informados
    private CallToolResult executar(Map<String, Object> argumentos) {
        CallToolRequest request = CallToolRequest.builder(NOME_FERRAMENTA).arguments(argumentos).build();
        return client.callTool(request);
    }

    private String texto(CallToolResult resultado) {
        return ((TextContent) resultado.content().get(0)).text();
    }

    //o servidor anuncia a ferramenta, com descrição e esquema
    @Test
    @DisplayName("O servidor disponibiliza a ferramenta gastos_por_categoria")
    void disponibilizarFerramenta() {
        Tool ferramenta = client.listTools().tools().stream()
                                .filter(tool -> NOME_FERRAMENTA.equals(tool.name()))
                                .findFirst()
                                .orElse(null);

        assertNotNull(ferramenta, "A ferramenta " + NOME_FERRAMENTA + " não foi encontrada.");
        assertNotNull(ferramenta.description());
        assertNotNull(ferramenta.inputSchema());
    }

    //sem caminho e sem conteúdo, a ferramenta devolve erro explicando o que falta
    @Test
    @DisplayName("Sem argumentos: devolve erro")
    void erroSemArgumentos() {
        CallToolResult resultado = executar(Map.of());

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains("Informe o caminho"));
    }

    //arquivo que não existe devolve erro
    @Test
    @DisplayName("Arquivo inexistente: devolve erro")
    void erroArquivoInexistente() {
        CallToolResult resultado = executar(Map.of("arquivo", "nao_existe.csv"));

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains("Arquivo não encontrado"));
    }

    //arquivo que não é .csv devolve erro
    @Test
    @DisplayName("Arquivo que não é CSV: devolve erro")
    void erroArquivoNaoCsv() {
        CallToolResult resultado = executar(Map.of("arquivo", new File("pom.xml").getAbsolutePath()));

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains(".csv"));
    }

    //sem a chave da IA no computador, a ferramenta avisa que a variável não está configurada
    @Test
    @DisplayName("Sem a chave GEMINI_API_KEY: devolve erro explicando")
    void erroSemChave() {
        assumeTrue(CHAVE == null || CHAVE.isBlank(), "Há chave configurada: teste não se aplica.");

        CallToolResult resultado = executar(Map.of("arquivo", new File(CSV_EXEMPLO).getAbsolutePath()));

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains("GEMINI_API_KEY"));
    }

    //com a chave, a ferramenta lê o CSV pelo caminho, classifica com a IA e devolve os totais
    @Test
    @DisplayName("Com a chave: executa pelo caminho do arquivo")
    void executarPeloCaminho() {
        assumeTrue(CHAVE != null && !CHAVE.isBlank(), "Sem GEMINI_API_KEY: teste com a IA ignorado.");

        CallToolResult resultado = executar(Map.of("arquivo", new File(CSV_EXEMPLO).getAbsolutePath()));

        assertFalse(resultado.isError(), texto(resultado));
        assertTrue(texto(resultado).contains("Total: R$"));
    }

    //com a chave, a ferramenta também funciona recebendo o texto do CSV (arquivo anexado no chat)
    @Test
    @DisplayName("Com a chave: executa pelo conteúdo do CSV")
    void executarPeloConteudo() {
        assumeTrue(CHAVE != null && !CHAVE.isBlank(), "Sem GEMINI_API_KEY: teste com a IA ignorado.");

        String conteudo = "date,title,amount\n2026-11-02,Uber *Trip,18.50\n";
        CallToolResult resultado = executar(Map.of("conteudo", conteudo));

        assertFalse(resultado.isError(), texto(resultado));
        assertTrue(texto(resultado).contains("Total: R$"));
    }
}
