# 💳 Analisador de Extrato Bancário: Servidor MCP

Servidor MCP (Model Context Protocol) desenvolvido em Java que permite a assistentes de IA, como o Claude Desktop, analisar extratos bancários, categorizando gastos com IA (Google Gemini) e gerando um resumo mensal para auxiliar no controle financeiro pessoal.

O projeto contém duas aplicações:
- **Servidor MCP:** disponibiliza as ferramentas de análise de extrato.
- **Cliente MCP:** aplicação de terminal capaz de listar e executar todas as ferramentas do servidor.

Trabalho final da disciplina de Serviços Web (IFPR).

## 👩‍💻 Autoras
- Fernanda Yumi Tomoi
- Victoria Camile Campos Bertuchi

## 🧰 Tecnologias
- Java 25
- Apache Maven
- MCP Java SDK
- Google Gemini (classificação dos gastos)
- JUnit (testes unitários e de integração)

## 🛠️ Ferramentas

| Ferramenta | O que faz |
|---|---|
| `gastos_por_categoria` | Lê o extrato, classifica cada compra com a IA (Delivery, Transporte, Mercado...) e devolve o total gasto em cada categoria. |
| `resumo_do_mes` | Gera um resumo dos gastos do extrato: total gasto, maior gasto, média por dia e categoria com maior gasto. |

## Ferramenta `gastos_por_categoria`

Lê um extrato bancário em CSV, classifica cada compra com IA (Google Gemini) e devolve o total gasto em cada categoria.

### Parâmetros

Informe **um** dos dois:

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `arquivo` | texto | Caminho completo do arquivo CSV no computador. Ex.: `C:\Users\Usuario\Downloads\extrato.csv` |
| `conteudo` | texto | O texto completo do CSV. Usado pelo Claude Desktop quando o arquivo é anexado no chat. |

Se os dois forem informados, `conteudo` é usado.

### Resultado (sucesso)

```
Gastos por categoria:
- DELIVERY: R$ 45.90
- TRANSPORTE: R$ 18.50
- STREAMING: R$ 39.90
Total: R$ 104.30
```

Só entram as saídas de dinheiro (compras, Pix enviado...). Entradas e linhas de saldo são ignoradas.
Se o extrato não tiver nenhum gasto, a resposta é `Nenhum gasto encontrado no extrato.`

### Possíveis erros

Em todos os casos a ferramenta responde com `isError = true` e uma mensagem:

| Mensagem | Quando acontece |
|---|---|
| `Informe o caminho do arquivo CSV ou anexe o arquivo no chat.` | Nenhum parâmetro foi enviado. |
| `Arquivo não encontrado: ...` | O caminho informado não existe. |
| `O arquivo precisa ser .csv: ...` | O arquivo não termina em `.csv`. |
| `A variável de ambiente GEMINI_API_KEY não está configurada. Veja o README.` | O servidor foi iniciado sem a chave da IA. |
| `Não foi possível ler o arquivo: ...` | Erro ao abrir o arquivo (permissão, arquivo em uso...). |
| `O arquivo não está num formato de extrato reconhecido: ...` | Falta coluna de data, descrição ou valor; ou data/valor em formato desconhecido. |
| `A IA não conseguiu classificar as compras: ...` | Sem internet, chave inválida ou Gemini fora do ar (o servidor tenta 4 vezes antes de desistir). |

## Ferramenta `resumo_do_mes`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `caminho_arquivo` | string | Sim | Caminho completo do arquivo CSV do extrato |

**Resultado:** total gasto, maior gasto, média de gasto por dia e categoria com maior gasto.

**Possíveis erros:**
- Arquivo não encontrado
- Arquivo vazio
- Formato do CSV inválido (colunas ausentes)
- Valor monetário inválido em alguma linha

## Requisitos

- Java 25 (JDK)
- Maven (ou o Maven que já vem no Eclipse)
- Claude Desktop
- Uma chave da API do Gemini (gratuita)

### Como conseguir a chave do Gemini

1. Acesse https://aistudio.google.com e entre com uma conta Google.
2. Clique em **Get API key** → **Create API key**.
3. Copie a chave. Ela **nunca** vai no código: o servidor lê a chave da variável de ambiente `GEMINI_API_KEY` (em `GastosPorCategoriaTool`, com `System.getenv("GEMINI_API_KEY")`).

### Onde colocar a chave

A chave precisa estar na variável `GEMINI_API_KEY` do lugar onde o servidor vai rodar:

| Onde o servidor roda | Onde colocar a chave |
|---|---|
| Claude Desktop | No `claude_desktop_config.json`, dentro de `"env"` (ver **Como configurar no Claude Desktop**). |
| Cliente MCP ou MCP Inspector (terminal) | No mesmo cmd, antes do comando: `set GEMINI_API_KEY=sua_chave` (vale só para aquele cmd). |
| Eclipse (Run As → Java Application) | **Run → Run Configurations...** → na lista da esquerda, abra **Java Application** e clique no nome da classe que você executa (ex.: `McpClient01SyncStdio`) → aba **Environment** → **Add...** → Name: `GEMINI_API_KEY`, Value: sua chave → **OK → Apply**. |
| Eclipse (testes JUnit com a IA) | **Run → Run Configurations...** → na lista da esquerda, abra **JUnit** e clique no nome do teste que você rodou (o nome é o da classe ou da pasta que você rodou, ex.: `GastosPorCategoriaToolIT`) → aba **Environment** → **Add...** → `GEMINI_API_KEY` e sua chave → **Apply**. Sem a chave, os 2 testes com a IA aparecem como ignorados. |
| Eclipse (Maven build) | Na janela do **Maven build...** → aba **Environment** → **Add...** → `GEMINI_API_KEY` e sua chave. |

Essas configurações ficam só no seu computador e não vão para o GitHub.

## Builds

Todos os builds podem ser feitos no Eclipse (botão direito no projeto → **Run As → Maven build...**, campo **Goals**, e na aba **JRE**, em **VM arguments**: `--enable-native-access=ALL-UNNAMED`) ou no terminal.

| Objetivo | Goals no Eclipse | Terminal | Resultado |
|---|---|---|---|
| Testes unitários | `clean test` | `mvn clean test` | Roda as classes `*Test` |
| Gerar o servidor | `clean package -Pserver -DskipTests` | `mvn clean package -Pserver -DskipTests` | `server/mcp_server01_server.jar` |
| Testes de integração | `clean verify -Pserver` | `mvn clean verify -Pserver` | Gera o servidor e roda as classes `*IT` contra ele |
| Gerar o cliente | `clean package -Pclient -DskipTests` | `mvn clean package -Pclient -DskipTests` | `target/dist-client/mcp_server01_client.jar` |

Os testes de integração também podem ser rodados no Eclipse, depois de gerar o servidor: botão direito na classe `GastosPorCategoriaToolIT` → **Run As → JUnit Test**.

**Antes de gerar o servidor, feche o Claude Desktop e o MCP Inspector**: se o servidor antigo estiver em uso, o Windows não deixa o Maven apagar a pasta `server`.

## Cliente MCP

O cliente fica em `br.edu.ifpr.mcp_server.main.McpClient01SyncStdio`. Ele sobe o servidor, mostra um menu com as ferramentas disponíveis, pede os parâmetros de cada uma (lidos do esquema JSON da ferramenta) e executa a escolhida. Por isso ele executa qualquer ferramenta do servidor.

Depois de gerar o servidor e o cliente (ver **Builds**), no terminal, a partir da pasta do projeto:

```
set GEMINI_API_KEY=sua_chave
cd target\dist-client
java -jar mcp_server01_client.jar
```

No menu: **1 - Ferramentas** → escolha `gastos_por_categoria` → em `arquivo`, informe o caminho completo do CSV → em `conteudo`, só aperte ENTER (é opcional).

## MCP Inspector

Requer Node.js. Na pasta do projeto, depois de gerar o servidor:

```
set GEMINI_API_KEY=sua_chave
npx @modelcontextprotocol/inspector java -jar server/mcp_server01_server.jar
```

O Inspector abre no navegador. Clique em **Connect** → aba **Tools** → **List Tools** → `gastos_por_categoria`, preencha `arquivo` com o caminho do CSV e clique em **Run Tool**.

## Como configurar no Claude Desktop

1. Abra o Claude Desktop → **Configurações → Aplicativo para desktop → Extensões → Gerenciar → Servidores MCP locais → Editar configuração**.
   (Em outras versões: Configurações → Desenvolvedor → Editar configuração.)
2. Isso abre o arquivo `claude_desktop_config.json`. Acrescente o bloco `mcpServers` (troque os caminhos e a chave pelos seus):

```json
{
  "mcpServers": {
    "analisador_extrato": {
      "command": "C:\\Program Files\\Java\\jdk-25\\bin\\java.exe",
      "args": [
        "-jar",
        "C:\\caminho\\do\\projeto\\server\\mcp_server01_server.jar"
      ],
      "env": {
        "GEMINI_API_KEY": "COLE_SUA_CHAVE_AQUI"
      }
    }
  }
}
```

- No Windows, as barras do caminho são duplas (`\\`).
- `command` é o caminho do `java.exe` do JDK 25.
- Se o arquivo já tiver outras configurações, coloque o bloco `"mcpServers": {...},` logo depois da primeira `{`.

3. **Feche o Claude Desktop por completo** (inclusive o ícone perto do relógio) e abra de novo.
4. Na lista de Servidores MCP locais, o `analisador_extrato` deve aparecer como **Em execução**.

## Como usar

No chat do Claude Desktop, há dois jeitos:

1. **Anexar o CSV** no chat e perguntar:
   > Quais foram meus gastos por categoria neste extrato?

   O Claude passa o texto do arquivo para a ferramenta (campo `conteudo`).

2. **Informar o caminho** do arquivo (melhor para extratos grandes):
   > Use a ferramenta gastos_por_categoria com o arquivo C:\Users\SeuUsuario\Downloads\extrato.csv

No Windows, para copiar o caminho: Shift + botão direito no arquivo → Copiar como caminho.

Em qualquer um dos jeitos, o Claude Desktop mostra que usou a ferramenta `gastos_por_categoria`.

Resposta esperada:

```
Gastos por categoria:
- DELIVERY: R$ 45.90
- TRANSPORTE: R$ 18.50
- FARMACIA: R$ 32.00
Total: R$ 96.40
```

A IA classifica uma compra por vez, então um extrato grande pode demorar. Para o primeiro teste, use um arquivo pequeno.

## Formatos de extrato aceitos

O servidor descobre sozinho o formato olhando a primeira linha (cabeçalho) do arquivo.

**1. CSV do Nubank** (cabeçalho `date,title,amount`)

```
date,title,amount
2026-11-01,Ifd*Saiko Sushi Comerc,45.90
2026-11-02,Uber *Trip,18.50
```

Nesse formato, o gasto vem **positivo**.

**2. Extrato de conta (formato genérico)**

As colunas são encontradas **pelo nome**, em qualquer ordem:

- Data: `Data`, `Date`, `Data Lançamento`
- Descrição: `Descrição`, `Histórico`, `Lançamento`, `Detalhes`, `Title` (se houver mais de uma, elas são juntadas)
- Valor: `Valor`, `Amount`, `Valor (R$)`
- Tipo (opcional): `Tipo Lançamento` / `Tipo` com `Entrada` ou `Saída`

Também aceita:
- separador `,` ou `;`;
- valor `1.234,56` ou `1234.56`;
- datas `05/11/2026`, `5/11/2026`, `05-11-2026`, `2026-11-05`, `2026/11/05`;
- linhas antes do cabeçalho (nome, agência, conta);
- arquivo salvo em UTF-8 ou com acentuação do Windows.

No extrato de conta, o gasto vem **negativo** (o dinheiro saiu da conta). Quando existe a coluna de tipo, ela é que decide: só as linhas de **Saída** contam como gasto. Linhas de saldo ("Saldo Anterior", "Saldo do dia") e entradas (Pix recebido, salário) são ignoradas.

Exemplo (Banco do Brasil):

```
Data,"Lançamento","Detalhes","N° documento","Valor","Tipo Lançamento"
10/11/2026,"Compra com Cartão","10/11 11:02 LOJA EXEMPLO","167349","-10,00","Saída"
10/11/2026,"Saldo do dia","","","924,33",""
```

## Categorias

`DELIVERY`, `TRANSPORTE`, `MERCADO`, `FARMACIA`, `SAUDE`, `STREAMING`, `ALIMENTACAO`, `EDUCACAO`, `LAZER`, `VESTUARIO`, `CONTAS_DA_CASA`, `PET`, `OUTROS`

## Arquitetura e padrões de projeto

Pacote `br.edu.ifpr.mcp_server.extrato` (regras do extrato) e `br.edu.ifpr.mcp_server.ia` (comunicação com a IA).

| Classe | Papel |
|---|---|
| `Transacao` | Uma linha do extrato: data, valor e descrição. |
| `Categoria` | As categorias possíveis. |
| `LeitorDeExtrato` | Interface: lê um arquivo e devolve a lista de transações. |
| `LeitorExtratoCsvNubank` | Lê o CSV do Nubank (`date,title,amount`). |
| `LeitorExtratoCsvGenerico` | Lê extratos de conta, achando as colunas pelo nome. |
| `LeitorDeExtratoFactory` | Olha o cabeçalho e escolhe o leitor certo. |
| `ClassificadorDeCategoria` | Interface: recebe uma transação e devolve a categoria. |
| `ClassificadorPorIA` | Classifica usando a IA. |
| `ClienteIA` | Interface: manda uma pergunta para a IA e devolve a resposta. |
| `ClienteGemini` | Implementação com o Google Gemini (tenta de novo até 4 vezes se o servidor estiver ocupado). |
| `CalculadoraGastosPorCategoria` | Soma os gastos de cada categoria (ignora entradas). |
| `GastosPorCategoriaTool` | A ferramenta MCP: junta tudo e devolve a resposta ao Claude. |

**Strategy**
- `LeitorDeExtrato`: dois leitores fazem a mesma tarefa de jeitos diferentes. No CSV do Nubank o gasto é positivo; no extrato de conta, negativo. Cada um tem a sua regra. O genérico atende os bancos que seguem a mesma convenção, sem repetir código (DRY).
- `ClassificadorDeCategoria`: a calculadora recebe "um classificador" sem saber qual é. Nos testes, passamos um classificador falso no lugar da IA.
- `ClienteIA`: dá para trocar o Gemini por outra IA criando uma nova classe, sem mexer no classificador.

**Factory**
- `LeitorDeExtratoFactory`: quem usa não precisa saber de qual banco é o extrato; a factory decide pelo cabeçalho.

**Injeção de dependência (pelo construtor)**
- `new CalculadoraGastosPorCategoria(classificador)`, `new ClassificadorPorIA(cliente)`: cada classe recebe pronto o que precisa, em vez de criar por dentro. É isso que permite testar com objetos falsos.

**SOLID**
- **S** (responsabilidade única): ler, classificar, falar com a IA e somar estão em classes separadas.
- **O** (aberto/fechado): para aceitar um formato novo de extrato, cria-se um novo `LeitorDeExtrato` e ensina-se a factory a reconhecê-lo, sem alterar as classes que já existem.
- **D** (inversão de dependência): as classes dependem das interfaces (`ClassificadorDeCategoria`, `ClienteIA`, `LeitorDeExtrato`), não das implementações.

## Testes

Os testes ficam em `src/test/java` e usam JUnit.

**Testes unitários** (`*Test`, rodam sem internet e sem chave):

| Classe | O que verifica |
|---|---|
| `LeitorExtratoCsvGenericoTest` | Formato do Banco do Brasil (Entrada/Saída), junção das colunas de descrição, linhas de saldo ignoradas, separador `;` e valor `1.234,56`, colunas em qualquer ordem, linhas antes do cabeçalho, os 4 formatos de data, arquivo com acentuação do Windows, arquivo vazio, coluna faltando e data inválida. |
| `LeitorExtratoCsvNubankTest` | Leitura do CSV do Nubank mantendo o sinal do valor. |
| `LeitorDeExtratoFactoryTest` | A factory escolhe o leitor Nubank para `date,title,amount` e o genérico para os outros cabeçalhos (inclusive arquivo vazio). |
| `ClassificadorPorIATest` | A resposta da IA vira a categoria certa; resposta inválida e erro da IA. Usa uma IA falsa. |
| `CalculadoraGastosPorCategoriaTest` | Soma por categoria, categorias separadas e entradas ignoradas. Usa um classificador falso. |

**Testes de integração** (`*IT`, sobem o servidor `server/mcp_server01_server.jar` e chamam a ferramenta pelo protocolo MCP):

| Classe | O que verifica |
|---|---|
| `GastosPorCategoriaToolIT` | A ferramenta é anunciada com descrição e esquema; erros sem parâmetros, arquivo inexistente, arquivo que não é CSV e sem a chave; e, quando `GEMINI_API_KEY` existe no computador, a execução completa pelo caminho e pelo conteúdo. |

Os dois testes que usam a IA de verdade só rodam se a variável `GEMINI_API_KEY` existir no ambiente; sem ela, aparecem como ignorados. O arquivo de exemplo usado está em `src/test/resources/extratos/fatura_nubank.csv`.

## Segurança

A chave da API **nunca** fica no código nem no GitHub. Ela é lida da variável de ambiente `GEMINI_API_KEY`, que é configurada no `claude_desktop_config.json` de cada computador.

## Limitações conhecidas

- A classificação depende da IA: sem chave, sem internet ou com o Gemini fora do ar, a ferramenta devolve uma mensagem de erro explicando o motivo.
- Um Pix enviado para uma pessoa ("Pix - Enviado - Fulano") não diz o que foi comprado, então a IA tende a classificar como `OUTROS`.
- Testado com o extrato do Banco do Brasil e o CSV do Nubank. Outros bancos funcionam se seguirem a convenção mais comum (débito negativo ou coluna Entrada/Saída). Um banco que traga o gasto positivo sem coluna de tipo seria lido sem erro, mas com total errado; para ele, basta criar um novo leitor e registrá-lo na factory.
- Extratos com valor em duas colunas ("Crédito" e "Débito") não são suportados.

## Melhorias futuras

- Leitores específicos para faturas de cartão de outros bancos.
- Suporte a extratos com valor em duas colunas ("Crédito" e "Débito").
- Leitura de outros formatos além de CSV (XLSX, OFX).
- Classificar várias compras numa única chamada à IA, para extratos grandes ficarem mais rápidos.