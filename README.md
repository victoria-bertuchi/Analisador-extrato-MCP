# 💳 Analisador de Extrato Bancário: Servidor MCP

Servidor MCP (Model Context Protocol) desenvolvido em Java que permite a assistentes de IA, como o Claude Desktop, analisar extratos bancários, categorizando gastos e gerando um resumo mensal para auxiliar no controle financeiro pessoal.

O projeto contém duas aplicações:
- **Servidor MCP:** disponibiliza as ferramentas de análise de extrato.
- **Cliente MCP:** aplicação de terminal capaz de listar e executar todas as ferramentas do servidor.

Trabalho final da disciplina de Serviços Web (IFPR).

## 👩‍💻 Autoras
- Fernanda Yumi Tomoi 
- Victoria Camile Campos Bertuchi

## 🛠️ Ferramentas

### `gastos_por_categoria`
Lê um extrato bancário em CSV e retorna o total gasto em cada categoria (mercado, transporte, delivery, etc.).

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `caminho_arquivo` | string | Sim | Caminho completo do arquivo CSV do extrato |

**Resultado:** lista de categorias com o valor total gasto em cada uma.

**Possíveis erros:**
- Arquivo não encontrado
- Arquivo vazio
- Formato do CSV inválido (colunas ausentes)
- Valor monetário inválido em alguma linha

### `resumo_do_mes`
Gera um resumo dos gastos do extrato.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `caminho_arquivo` | string | Sim | Caminho completo do arquivo CSV do extrato |

**Resultado:** total gasto, maior gasto, média de gasto por dia e categoria com maior gasto.

**Possíveis erros:** os mesmos da ferramenta `gastos_por_categoria`.

## 🧰 Tecnologias
- Java 25
- Apache Maven
- MCP Java SDK
- JUnit (testes unitários e de integração)

## ⚙️ Como executar
_(em construção)_