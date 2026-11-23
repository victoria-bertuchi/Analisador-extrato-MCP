package br.edu.ifpr.mcp_server.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Testes do ClienteGemini contra um servidor HTTP falso, criado na própria
 * máquina: rodam sem internet e sem chave.
 */
class ClienteGeminiTest {

	private static final String RESPOSTA_OK = "{\"choices\":[{\"message\":{\"content\":\"1=TRANSPORTE\"}}]}";

	private final AtomicInteger chamadas = new AtomicInteger();
	private final AtomicReference<String> autorizacao = new AtomicReference<>();
	private HttpServer servidor;

	@AfterEach
	void pararServidor() {
		if (servidor != null) {
			servidor.stop(0);
		}
	}

	// sobe o servidor falso. Cada chamada recebe o próximo código da lista (o
	// último se repete)
	private String iniciarServidor(int... codigos) throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		servidor.createContext("/", troca -> {
			autorizacao.set(troca.getRequestHeaders().getFirst("Authorization"));
			int indice = Math.min(chamadas.getAndIncrement(), codigos.length - 1);
			int codigo = codigos[indice];
			String corpo = codigo == 200 ? RESPOSTA_OK : "{\"error\":\"falhou\"}";
			byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
			troca.getResponseHeaders().add("Content-Type", "application/json");
			troca.sendResponseHeaders(codigo, bytes.length);
			try (OutputStream saida = troca.getResponseBody()) {
				saida.write(bytes);
			}
		});
		servidor.start();
		return "http://localhost:" + servidor.getAddress().getPort() + "/chat";
	}

	private ClienteGemini cliente(String url) {
		return new ClienteGemini("chave-teste", url, "modelo-teste");
	}

	@Test
	@DisplayName("Resposta 200: devolve o texto e envia a chave no cabeçalho")
	void respostaOk() throws Exception {
		ClienteGemini cliente = cliente(iniciarServidor(200));

		String resposta = cliente.perguntar("regras", "1. Uber");

		assertEquals("1=TRANSPORTE", resposta);
		assertEquals("Bearer chave-teste", autorizacao.get());
		assertEquals(1, chamadas.get());
	}

	@Test
	@DisplayName("429 seguido de 200: tenta de novo e devolve o texto")
	void tentaDeNovoQuandoOcupado() throws Exception {
		ClienteGemini cliente = cliente(iniciarServidor(429, 200));

		String resposta = cliente.perguntar("regras", "1. Uber");

		assertEquals("1=TRANSPORTE", resposta);
		assertEquals(2, chamadas.get());
	}

	@Test
	@DisplayName("503 o tempo todo: desiste depois de 3 tentativas")
	void desisteDepoisDasTentativas() throws Exception {
		ClienteGemini cliente = cliente(iniciarServidor(503));

		IllegalStateException erro = assertThrows(IllegalStateException.class,
				() -> cliente.perguntar("regras", "1. Uber"));

		assertTrue(erro.getMessage().contains("503"));
		assertEquals(3, chamadas.get());
	}

	@Test
	@DisplayName("401 (chave inválida): erro na hora, sem tentar de novo")
	void naoTentaDeNovoEmOutrosErros() throws Exception {
		ClienteGemini cliente = cliente(iniciarServidor(401));

		IllegalStateException erro = assertThrows(IllegalStateException.class,
				() -> cliente.perguntar("regras", "1. Uber"));

		assertTrue(erro.getMessage().contains("401"));
		assertEquals(1, chamadas.get());
	}
}