package br.ufscar.dc.compiladores.jander;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TabelaDeSimbolos {

    public enum TipoJander {
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        PONTEIRO,
        REGISTRO,       // Para instâncias de registros anônimos
        REGISTRO_TIPO,  // Para definições de tipos de registro nomeados (e.g., 'tipo MeuPonto: registro...')
        ENDERECO,
        FUNCAO,
        PROCEDIMENTO,
        INVALIDO
    }

    public static class EntradaTabelaDeSimbolos {
        public final String nome;
        public final TipoJander tipo;
        public final List<TabelaDeSimbolos.TipoJander> parametros;
        public final TipoJander tipoRetorno;
        public Map<String, TipoJander> camposRegistro; // Para instâncias de registros
        public Map<String, TipoJander> camposTipoRegistro; // Para definições de tipos de registro

        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo) {
            this.nome = nome;
            this.tipo = tipo;
            this.parametros = null;
            this.tipoRetorno = null;
            this.camposRegistro = (tipo == TipoJander.REGISTRO || tipo == TipoJander.REGISTRO_TIPO) ? new HashMap<>() : null; // Inicializa para ambos os tipos de registro
            this.camposTipoRegistro = (tipo == TipoJander.REGISTRO_TIPO) ? new HashMap<>() : null;
        }

        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo, 
                                      List<TabelaDeSimbolos.TipoJander> parametros, 
                                      TipoJander tipoRetorno) {
            this.nome = nome;
            this.tipo = tipo;
            this.parametros = parametros;
            this.tipoRetorno = tipoRetorno;
            this.camposRegistro = null; // Funções e procedimentos não têm campos de registro
            this.camposTipoRegistro = null;
        }
    }

    private final Map<String, EntradaTabelaDeSimbolos> tabela;
    private final Stack<Map<String, EntradaTabelaDeSimbolos>> escopos;
    private final Stack<Boolean> escoposFuncao;
    private TipoJander tipoRetornoAtual;

    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
        this.escopos = new Stack<>();
        this.escoposFuncao = new Stack<>();
        this.tipoRetornoAtual = TipoJander.INVALIDO;
        // Inicia com o escopo global
        escopos.push(new HashMap<>());
        escoposFuncao.push(false);
    }

    public void adicionar(String nome, TipoJander tipo) {
        escopos.peek().put(nome, new EntradaTabelaDeSimbolos(nome, tipo));
    }

    // Usado para adicionar campos a uma INSTÂNCIA de registro (e.g., 'ponto1.x')
    public void adicionarCampoRegistroAInstancia(String nomeInstanciaRegistro, String nomeCampo, TipoJander tipoCampo) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeInstanciaRegistro)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeInstanciaRegistro);
                if (entrada.tipo == TipoJander.REGISTRO || entrada.tipo == TipoJander.REGISTRO_TIPO) {
                    if (entrada.camposRegistro == null) { // Garante que o mapa existe
                        entrada.camposRegistro = new HashMap<>();
                    }
                    entrada.camposRegistro.put(nomeCampo, tipoCampo);
                    return;
                }
            }
        }
    }

    // Usado para adicionar campos a uma DEFINIÇÃO DE TIPO de registro (e.g., 'tipo MeuPonto: registro x: real fim_registro')
    public void adicionarCampoRegistroATipo(String nomeTipoRegistro, String nomeCampo, TipoJander tipoCampo) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeTipoRegistro)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeTipoRegistro);
                if (entrada.tipo == TipoJander.REGISTRO_TIPO) {
                    if (entrada.camposTipoRegistro == null) { // Garante que o mapa existe
                        entrada.camposTipoRegistro = new HashMap<>();
                    }
                    entrada.camposTipoRegistro.put(nomeCampo, tipoCampo);
                    return;
                }
            }
        }
    }

    public Map<String, TipoJander> obterCamposDoTipoRegistro(String nomeTipoRegistro) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeTipoRegistro)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeTipoRegistro);
                if (entrada.tipo == TipoJander.REGISTRO_TIPO && entrada.camposTipoRegistro != null) {
                    return new HashMap<>(entrada.camposTipoRegistro);
                }
            }
        }
        return null; // Retorna null se o tipo de registro não for encontrado ou não tiver campos
    }

    public void adicionarFuncao(String nome, TipoJander tipoRetorno, List<TabelaDeSimbolos.TipoJander> parametros) {
        escopos.peek().put(nome, new EntradaTabelaDeSimbolos(nome, TipoJander.FUNCAO, parametros, tipoRetorno));
    }

    public boolean existe(String nome) {
        if (nome.contains(".")) {
            String[] partes = nome.split("\\.");
            String nomeVar = partes[0];
            String nomeCampo = partes[1];
            
            // Verifica se a variável principal (e.g., ponto1) existe
            for (int i = escopos.size() - 1; i >= 0; i--) {
                if (escopos.get(i).containsKey(nomeVar)) {
                    EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeVar);
                    // Verifica se é uma instância de registro (REGISTRO ou REGISTRO_TIPO) e se tem o campo
                    if ((entrada.tipo == TipoJander.REGISTRO || entrada.tipo == TipoJander.REGISTRO_TIPO) && entrada.camposRegistro != null && entrada.camposRegistro.containsKey(nomeCampo)) {
                        return true;
                    }
                    // Adição: se a variável é do tipo REGISTRO_TIPO, verifique os campos da *definição* do tipo
                    // Isso pode ser necessário se você quiser permitir 'MeuTipoDePonto.x' em algum contexto
                    // Mas para o caso 'ponto1.x', o camposRegistro da instância é o que importa
                }
            }
            return false;
        }
        
        // Verificação normal para variáveis/tipos/funções não-registro
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nome)) {
                return true;
            }
        }
        return false;
    }

    public TipoJander verificar(String nome) {
        if (nome.contains(".")) {
            String[] partes = nome.split("\\.");
            String nomeVar = partes[0];
            String nomeCampo = partes[1];
            
            for (int i = escopos.size() - 1; i >= 0; i--) {
                if (escopos.get(i).containsKey(nomeVar)) {
                    EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeVar);
                    // Verifica se é uma instância de registro e retorna o tipo do campo
                    if ((entrada.tipo == TipoJander.REGISTRO || entrada.tipo == TipoJander.REGISTRO_TIPO) && entrada.camposRegistro != null) {
                        return entrada.camposRegistro.getOrDefault(nomeCampo, TipoJander.INVALIDO);
                    }
                }
            }
            return TipoJander.INVALIDO;
        }
        
        // Verificação normal para variáveis/tipos/funções não-registro
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nome)) {
                return escopos.get(i).get(nome).tipo;
            }
        }
        return TipoJander.INVALIDO;
    }

    public List<TabelaDeSimbolos.TipoJander> obterParametros(String nome) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nome)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nome);
                if (entrada.parametros != null) {
                    return entrada.parametros;
                }
            }
        }
        return new ArrayList<>();
    }

    public void novoEscopo(boolean isFuncao) {
        escopos.push(new HashMap<>());
        escoposFuncao.push(isFuncao);
        if (isFuncao) {
            tipoRetornoAtual = TipoJander.INVALIDO;
        }
    }

    public void abandonarEscopo() {
        escopos.pop();
        escoposFuncao.pop();
        if (!escoposFuncao.empty() && !escoposFuncao.peek()) { // Verifica se não está vazio e se o topo não é uma função
            tipoRetornoAtual = TipoJander.INVALIDO;
        }
    }

    public boolean estaEmFuncao() {
        return !escoposFuncao.empty() && escoposFuncao.peek();
    }

    public TipoJander obterTipoRetornoFuncaoAtual() {
        return tipoRetornoAtual;
    }

    public void setTipoRetornoFuncaoAtual(TipoJander tipo) {
        this.tipoRetornoAtual = tipo;
    }
}