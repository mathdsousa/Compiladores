package br.ufscar.dc.compiladores.jander;

import java.util.HashMap;
import java.util.Map;

public class TabelaDeSimbolos {

    public enum TipoJander {
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        INVALIDO
    }

    public static class EntradaTabelaDeSimbolos {
        public final String nome;
        public final TipoJander tipo;

        public EntradaTabelaDeSimbolos(String nome, TipoJander tipo) {
            this.nome = nome;
            this.tipo = tipo;
        }
    }

    private final Map<String, EntradaTabelaDeSimbolos> tabela;

    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }

    public void adicionar(String nome, TipoJander tipo) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo));
    }

    public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }

    public TipoJander verificar(String nome) {
        EntradaTabelaDeSimbolos entrada = tabela.get(nome);
        if (entrada != null) {
            return entrada.tipo;
        }
        return TipoJander.INVALIDO;
    }

    public Map<String, TipoJander> getSimbolos() {
        Map<String, TipoJander> copia = new HashMap<>();
        for (var entrada : tabela.entrySet()) {
            copia.put(entrada.getKey(), entrada.getValue().tipo);
        }
        return copia;
    }
}
