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
        public List<Integer> dimensoes; // Adicionado: para armazenar as dimensões do array

        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo) {
            this.nome = nome;
            this.tipo = tipo;
            this.parametros = null;
            this.tipoRetorno = null;
            this.camposRegistro = (tipo == TipoJander.REGISTRO || tipo == TipoJander.REGISTRO_TIPO) ? new HashMap<>() : null;
            this.camposTipoRegistro = (tipo == TipoJander.REGISTRO_TIPO) ? new HashMap<>() : null;
            this.dimensoes = null; // Inicializa nulo para não arrays
        }

        // Novo construtor para arrays
        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo, List<Integer> dimensoes) {
            this.nome = nome;
            this.tipo = tipo;
            this.parametros = null;
            this.tipoRetorno = null;
            this.camposRegistro = null;
            this.camposTipoRegistro = null;
            this.dimensoes = dimensoes;
        }

        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo, 
                                      List<TabelaDeSimbolos.TipoJander> parametros, 
                                      TipoJander tipoRetorno) {
            this.nome = nome;
            this.tipo = tipo;
            this.parametros = parametros;
            this.tipoRetorno = tipoRetorno;
            this.camposRegistro = null;
            this.camposTipoRegistro = null;
            this.dimensoes = null;
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
        escopos.push(new HashMap<>());
        escoposFuncao.push(false);
    }

    public void adicionar(String nome, TipoJander tipo) {
        escopos.peek().put(nome, new EntradaTabelaDeSimbolos(nome, tipo));
    }

    // Novo método adicionar para arrays
    public void adicionarArray(String nome, TipoJander tipo, List<Integer> dimensoes) {
        escopos.peek().put(nome, new EntradaTabelaDeSimbolos(nome, tipo, dimensoes));
    }

    public void adicionarCampoRegistroAInstancia(String nomeInstanciaRegistro, String nomeCampo, TipoJander tipoCampo) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeInstanciaRegistro)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeInstanciaRegistro);
                if (entrada.tipo == TipoJander.REGISTRO || entrada.tipo == TipoJander.REGISTRO_TIPO) {
                    if (entrada.camposRegistro == null) {
                        entrada.camposRegistro = new HashMap<>();
                    }
                    entrada.camposRegistro.put(nomeCampo, tipoCampo);
                    return;
                }
            }
        }
    }

    public void adicionarCampoRegistroATipo(String nomeTipoRegistro, String nomeCampo, TipoJander tipoCampo) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeTipoRegistro)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeTipoRegistro);
                if (entrada.tipo == TipoJander.REGISTRO_TIPO) {
                    if (entrada.camposTipoRegistro == null) {
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
        return null; 
    }

    public void adicionarFuncao(String nome, TipoJander tipoRetorno, List<TabelaDeSimbolos.TipoJander> parametros) {
        escopos.peek().put(nome, new EntradaTabelaDeSimbolos(nome, 
                                                            (tipoRetorno == TipoJander.INVALIDO ? TipoJander.PROCEDIMENTO : TipoJander.FUNCAO),
                                                            parametros, 
                                                            tipoRetorno));
    }

    // Modificado o método existe para considerar arrays e registros de forma mais robusta
    public boolean existe(String nomeCompleto) {
        String nomeBase = nomeCompleto;

        if (nomeCompleto.contains(".")) { 
            nomeBase = nomeCompleto.split("\\.")[0];
        } else if (nomeCompleto.contains("[")) { 
            nomeBase = nomeCompleto.substring(0, nomeCompleto.indexOf('['));
        }

        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeBase)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeBase);

                if (nomeCompleto.contains(".")) { 
                    String nomeCampo = nomeCompleto.split("\\.")[1];
                    return (entrada.tipo == TipoJander.REGISTRO || entrada.tipo == TipoJander.REGISTRO_TIPO) &&
                           entrada.camposRegistro != null && entrada.camposRegistro.containsKey(nomeCampo);
                } else if (nomeCompleto.contains("[")) { 
                    return entrada.dimensoes != null;
                } else { 
                    return true;
                }
            }
        }
        return false;
    }

    // Modificado o método verificar para considerar arrays e registros de forma mais robusta
    public TipoJander verificar(String nomeCompleto) {
        String nomeBase = nomeCompleto;

        if (nomeCompleto.contains(".")) {
            nomeBase = nomeCompleto.split("\\.")[0];
        } else if (nomeCompleto.contains("[")) {
            nomeBase = nomeCompleto.substring(0, nomeCompleto.indexOf('['));
        }
        
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nomeBase)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nomeBase);

                if (nomeCompleto.contains(".")) { 
                    String nomeCampo = nomeCompleto.split("\\.")[1];
                    if ((entrada.tipo == TipoJander.REGISTRO || entrada.tipo == TipoJander.REGISTRO_TIPO) && entrada.camposRegistro != null) {
                        return entrada.camposRegistro.getOrDefault(nomeCampo, TipoJander.INVALIDO);
                    }
                } else if (nomeCompleto.contains("[")) { 
                    if (entrada.dimensoes != null) {
                         return entrada.tipo; 
                    } else {
                        return TipoJander.INVALIDO; 
                    }
                } else { 
                    return entrada.tipo;
                }
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

    public TipoJander obterTipoRetorno(String nome) {
        for (int i = escopos.size() - 1; i >= 0; i--) {
            if (escopos.get(i).containsKey(nome)) {
                EntradaTabelaDeSimbolos entrada = escopos.get(i).get(nome);
                if (entrada.tipo == TipoJander.FUNCAO) {
                    return entrada.tipoRetorno;
                }
            }
        }
        return TipoJander.INVALIDO;
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
        if (!escoposFuncao.empty() && !escoposFuncao.peek()) { 
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