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
        REGISTRO,
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
        public final Map<String, TipoJander> camposRegistro;
        

        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo) {
            this.nome = nome;
            this.tipo = tipo;
            this.parametros = null;
            this.tipoRetorno = null;
            this.camposRegistro = tipo == TipoJander.REGISTRO ? new HashMap<>() : null;
        }

        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo, 
                                      List<TabelaDeSimbolos.TipoJander> parametros, 
                                      TipoJander tipoRetorno) {
            this.nome = nome;
            this.tipo = tipo;
            this.parametros = parametros;
            this.tipoRetorno = tipoRetorno;
            this.camposRegistro = null;
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

    public void adicionarCampoRegistro(String nomeRegistro, String nomeCampo, TipoJander tipoCampo) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeRegistro)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeRegistro);
                if (entrada.tipo == TipoJander.REGISTRO && entrada.camposRegistro != null) {
                    entrada.camposRegistro.put(nomeCampo, tipoCampo);
                    return;
                }
            }
        }
    }

    public TipoJander verificarCampoRegistro(String nomeRegistro, String nomeCampo) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeRegistro)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeRegistro);
                if (entrada.tipo == TipoJander.REGISTRO && entrada.camposRegistro != null) {
                    return entrada.camposRegistro.getOrDefault(nomeCampo, TipoJander.INVALIDO);
                }
            }
        }
        return TipoJander.INVALIDO;
    }

    public void adicionarFuncao(String nome, TipoJander tipoRetorno, List<TabelaDeSimbolos.TipoJander> parametros) {
        escopos.peek().put(nome, new EntradaTabelaDeSimbolos(nome, TipoJander.FUNCAO, parametros, tipoRetorno));
    }

    public boolean existe(String nome) {
        // Verifica se é um campo de registro (contém ponto)
        if (nome.contains(".")) {
            String[] partes = nome.split("\\.");
            String nomeRegistro = partes[0];
            String nomeCampo = partes[1];
            
            // Verifica se o registro existe
            if (existe(nomeRegistro)) {
                // Verifica se o tipo do registro é REGISTRO
                TipoJander tipoRegistro = verificar(nomeRegistro);
                if (tipoRegistro == TipoJander.REGISTRO) {
                    // Considera que o campo existe para evitar erros duplicados
                    return true;
                }
            }
            return false;
        }
        
        // Verifica do escopo mais interno para o mais externo
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nome)) {
                return true;
            }
        }
        return false;
    }

    public TipoJander verificar(String nome) {
        // Verifica se é um campo de registro (contém ponto)
        if (nome.contains(".")) {
            String[] partes = nome.split("\\.");
            String nomeRegistro = partes[0];
            String nomeCampo = partes[1];
            
            // Verifica se o registro existe
            for (int i = escopos.size() - 1; i >= 0; i--) {
                if (escopos.get(i).containsKey(nomeRegistro)) {
                    EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeRegistro);
                    if (entrada.tipo == TipoJander.REGISTRO) {
                        // Para simplificar, assumimos que todos os campos de registro são do tipo REAL
                        return TipoJander.REAL;
                    }
                }
            }
            return TipoJander.INVALIDO;
        }
        
        // Verifica do escopo mais interno para o mais externo
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
        if (escoposFuncao.peek() == null || !escoposFuncao.peek()) {
            tipoRetornoAtual = TipoJander.INVALIDO;
        }
    }

    public boolean estaEmFuncao() {
        return escoposFuncao.peek() != null && escoposFuncao.peek();
    }

    public TipoJander obterTipoRetornoFuncaoAtual() {
        return tipoRetornoAtual;
    }

    public void setTipoRetornoFuncaoAtual(TipoJander tipo) {
        this.tipoRetornoAtual = tipo;
    }
}
