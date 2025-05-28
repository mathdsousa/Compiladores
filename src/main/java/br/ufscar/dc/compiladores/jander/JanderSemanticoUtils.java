package br.ufscar.dc.compiladores.jander;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.Token; // Adicionado para Map

public class JanderSemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<>();
    private static Set<String> variaveisComErro = new HashSet<>();
    private static Set<String> tiposComErro = new HashSet<>();
    
    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    public static void resetarErros() {
        errosSemanticos.clear();
        variaveisComErro.clear();
        tiposComErro.clear();
    }
    
    public static boolean compatibilidade(TabelaDeSimbolos tabela, 
                                      TabelaDeSimbolos.TipoJander tipo1, 
                                      TabelaDeSimbolos.TipoJander tipo2) {
        if (tipo1 == tipo2) {
            return true;
        }

        // Casos especiais para REGISTRO e REGISTRO_TIPO
        if ((tipo1 == TabelaDeSimbolos.TipoJander.REGISTRO || tipo1 == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) &&
            (tipo2 == TabelaDeSimbolos.TipoJander.REGISTRO || tipo2 == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO)) {
            return true; // Considera registros do mesmo "tipo" compatíveis
        }

        if ((tipo1 == TabelaDeSimbolos.TipoJander.INTEIRO && tipo2 == TabelaDeSimbolos.TipoJander.REAL) ||
            (tipo1 == TabelaDeSimbolos.TipoJander.REAL && tipo2 == TabelaDeSimbolos.TipoJander.INTEIRO)) {
            return true;
        }

        return false;
    }

    public static boolean adicionarErroSeNecessario(String nome) {
        if (!variaveisComErro.contains(nome)) {    
            variaveisComErro.add(nome);
            return true;
        }
        return false;
    }
    
    // Verificação de tipos para cada construção da gramática

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.Tipo_basicoContext ctx) {
        if (ctx == null) return TabelaDeSimbolos.TipoJander.INVALIDO;
        
        switch (ctx.getText()) {
            case "literal": return TabelaDeSimbolos.TipoJander.LITERAL;
            case "inteiro": return TabelaDeSimbolos.TipoJander.INTEIRO;
            case "real": return TabelaDeSimbolos.TipoJander.REAL;
            case "logico": return TabelaDeSimbolos.TipoJander.LOGICO;
            default: return TabelaDeSimbolos.TipoJander.INVALIDO;
        }
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.Tipo_estendidoContext ctx) {
        if (ctx == null) return TabelaDeSimbolos.TipoJander.INVALIDO;
        
        TabelaDeSimbolos.TipoJander tipo = verificarTipo(tabela, ctx.tipo_basico_ident());
        if (ctx.PONTEIRO() != null) {
            // Para ponteiros, precisamos verificar se o tipo base é válido
            if (tipo == TabelaDeSimbolos.TipoJander.INVALIDO) {
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            return TabelaDeSimbolos.TipoJander.PONTEIRO;
        }
        return tipo;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                  JanderParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null) {
            return verificarTipo(tabela, ctx.tipo_basico());
        } else if (ctx.IDENT() != null) {
            String tipoNome = ctx.IDENT().getText();
            if (!tabela.existe(tipoNome)) {
                if(!tiposComErro.contains(tipoNome)) {
                    tiposComErro.add(tipoNome);
                    adicionarErroSemantico(ctx.start, "tipo " + tipoNome + " nao declarado");
                }
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            return tabela.verificar(tipoNome);
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
    
    // MODIFICAÇÃO AQUI
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            String nome = ctx.identificador().getText();
            if (!tabela.existe(nome)) {
                if(adicionarErroSeNecessario(nome)) {
                    adicionarErroSemantico(ctx.getStart(), "identificador " + nome + " nao declarado");
                }
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            return tabela.verificar(nome);
        }
        else if (ctx.NUM_INT() != null) {
            return TabelaDeSimbolos.TipoJander.INTEIRO;
        }
        else if (ctx.NUM_REAL() != null) {
            return TabelaDeSimbolos.TipoJander.REAL;
        }
        // Handling for (expressao)
        else if (ctx.expressao() != null && !ctx.expressao().isEmpty() && ctx.IDENT() == null) {
            TabelaDeSimbolos.TipoJander tipo = null;
            for (var exp : ctx.expressao()) {
                TabelaDeSimbolos.TipoJander aux = verificarTipo(tabela, exp);
                if (tipo == null) {
                    tipo = aux;
                }
                else if (aux != TabelaDeSimbolos.TipoJander.INVALIDO && !compatibilidade(tabela, tipo, aux)) {
                    adicionarErroSemantico(ctx.getStart(), "Expressao incompativel dentro de parcela unária");
                    tipo = TabelaDeSimbolos.TipoJander.INVALIDO;
                }
            }
            return tipo;
        }
        // Handling for function calls: IDENT ABREPAR expressao (VIRG expressao)* FECHAPAR
        else if (ctx.IDENT() != null && ctx.ABREPAR() != null && ctx.FECHAPAR() != null) {
            String nomeFunc = ctx.IDENT().getText();
            if (!tabela.existe(nomeFunc)) {
                if (adicionarErroSeNecessario(nomeFunc)) {
                    adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeFunc + " nao declarado");
                }
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }

            TabelaDeSimbolos.TipoJander tipoNoTabela = tabela.verificar(nomeFunc);

            // Check if it's actually a FUNCAO or PROCEDIMENTO (though PROCEDIMENTO typically doesn't return a value for expressions)
            if (tipoNoTabela != TabelaDeSimbolos.TipoJander.FUNCAO && tipoNoTabela != TabelaDeSimbolos.TipoJander.PROCEDIMENTO) {
                 adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeFunc + " nao e uma funcao ou procedimento");
                 return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            
            List<TabelaDeSimbolos.TipoJander> tiposParametrosEsperados = tabela.obterParametros(nomeFunc);
            List<JanderParser.ExpressaoContext> argumentosPassados = ctx.expressao();

            if (tiposParametrosEsperados.size() != argumentosPassados.size()) {
                adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
                // Continue with type checking even if parameter count is wrong, to find more errors if any.
            } else {
                for (int i = 0; i < tiposParametrosEsperados.size(); i++) {
                    TabelaDeSimbolos.TipoJander tipoParamEsperado = tiposParametrosEsperados.get(i);
                    TabelaDeSimbolos.TipoJander tipoArgumentoPassado = verificarTipo(tabela, argumentosPassados.get(i));

                    if (!compatibilidade(tabela, tipoParamEsperado, tipoArgumentoPassado)) {
                        adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
                        break; // Stop checking after the first mismatch for this function
                    }
                }
            }
            // For a function, return its declared return type. For a procedure, it's INVALIDO in an expression context.
            return (tipoNoTabela == TabelaDeSimbolos.TipoJander.FUNCAO) ? tabela.obterTipoRetornoFuncaoAtual() : TabelaDeSimbolos.TipoJander.INVALIDO;
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }


    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.Parcela_nao_unarioContext ctx) {
        if (ctx.ENDERECO() != null && ctx.identificador() != null) {
            String nome = ctx.identificador().getText();
            if (!tabela.existe(nome)) {
                if(adicionarErroSeNecessario(nome)) {
                    adicionarErroSemantico(ctx.getStart(), "identificador " + nome + " nao declarado");
                }
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            // Retorna o tipo ENDERECO para operações com &
            return TabelaDeSimbolos.TipoJander.ENDERECO;
        } else if (ctx.CADEIA() != null) {
            return TabelaDeSimbolos.TipoJander.LITERAL;
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            return verificarTipo(tabela, ctx.parcela_unario());
        } else if (ctx.parcela_nao_unario() != null) {
            return verificarTipo(tabela, ctx.parcela_nao_unario());
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                  JanderParser.Exp_aritmeticaContext ctx) {
        TabelaDeSimbolos.TipoJander tipo = null;
        for (var termo : ctx.termo()) {
            TabelaDeSimbolos.TipoJander aux = verificarTipo(tabela, termo);
            if (tipo == null) {
                tipo = aux;
            } else if (!compatibilidade(tabela, tipo, aux) && aux != TabelaDeSimbolos.TipoJander.INVALIDO) {
                tipo = TabelaDeSimbolos.TipoJander.INVALIDO;
            }
        }

        return tipo != null ? tipo : TabelaDeSimbolos.TipoJander.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                     JanderParser.TermoContext ctx) {
        TabelaDeSimbolos.TipoJander tipo = null;
        for (var fator : ctx.fator()) {
            TabelaDeSimbolos.TipoJander aux = verificarTipo(tabela, fator);
            if (tipo == null) {
                tipo = aux;
            } else if (!compatibilidade(tabela, tipo, aux) && aux!= TabelaDeSimbolos.TipoJander.INVALIDO) {
                    adicionarErroSemantico(ctx.start, "tipos incompativeis no termo ");
                    return TabelaDeSimbolos.TipoJander.INVALIDO;
                }
            }
        
        return tipo != null ? tipo : TabelaDeSimbolos.TipoJander.INVALIDO;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                         JanderParser.FatorContext ctx) {
        TabelaDeSimbolos.TipoJander tipo = null;
        for (var parcela : ctx.parcela()) {
            TabelaDeSimbolos.TipoJander aux = verificarTipo(tabela, parcela);

            if (aux == TabelaDeSimbolos.TipoJander.INVALIDO) {
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }

            if (tipo == null) {
                tipo = aux;
            } else if (!compatibilidade(tabela, tipo, aux) && aux != TabelaDeSimbolos.TipoJander.INVALIDO) {
                adicionarErroSemantico(ctx.start, "fator contem tipos incompativeis");
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
        }
        
        return tipo;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                      JanderParser.Exp_relacionalContext ctx) {
        TabelaDeSimbolos.TipoJander tipo = verificarTipo(tabela, ctx.exp_aritmetica(0));

        if (ctx.op_relacional() != null) {
            TabelaDeSimbolos.TipoJander aux = verificarTipo(tabela, ctx.exp_aritmetica(1));
            if (!compatibilidade(tabela, tipo, aux)) {
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            return TabelaDeSimbolos.TipoJander.LOGICO;
        }

        return tipo;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            return verificarTipo(tabela, ctx.exp_relacional());
        } else if (ctx.VERDADEIRO() != null || ctx.FALSO() != null) {
            return TabelaDeSimbolos.TipoJander.LOGICO;
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.Fator_logicoContext ctx) {
        if (ctx.parcela_logica() != null) {
            return verificarTipo(tabela, ctx.parcela_logica());
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.Termo_logicoContext ctx) {
        TabelaDeSimbolos.TipoJander tipo = null;
        for (var fator : ctx.fator_logico()) {
            TabelaDeSimbolos.TipoJander aux = verificarTipo(tabela, fator);
            if (tipo == null) {
                tipo = aux;
            } else if (!compatibilidade(tabela, tipo, aux)) {
                tipo = TabelaDeSimbolos.TipoJander.INVALIDO;
            }
        }
        return tipo;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.ExpressaoContext ctx) {
        TabelaDeSimbolos.TipoJander tipo = null;
        for (var termo : ctx.termo_logico()) {
            TabelaDeSimbolos.TipoJander aux = verificarTipo(tabela, termo);
            if (tipo == null) {
                tipo = aux;
            } else if (!compatibilidade(tabela, tipo, aux)) {
                tipo = TabelaDeSimbolos.TipoJander.INVALIDO;
            }
        }
        return tipo;
    }
    
    // Método auxiliar para obter o tipo base de um ponteiro
    public static TabelaDeSimbolos.TipoJander obterTipoBaseDoPonteiro(TabelaDeSimbolos tabela, String nomePonteiro) {
        // Implementação simplificada - em um compilador real, isso seria mais complexo
        // e envolveria a análise da declaração do ponteiro
        // Para este exemplo, assumimos que o tipo base de um ponteiro é INTEIRO ou REAL
        TabelaDeSimbolos.TipoJander tipoDoPonteiro = tabela.verificar(nomePonteiro);
        if (tipoDoPonteiro == TabelaDeSimbolos.TipoJander.PONTEIRO) {
            // Em um cenário real, você teria que armazenar o tipo base do ponteiro
            // quando ele é declarado. Por simplicidade, vamos tentar inferir ou usar um padrão.
            // Se o nome do ponteiro contiver "int", assumimos INTEIRO, senão REAL.
            if (nomePonteiro.toLowerCase().contains("int")) {
                return TabelaDeSimbolos.TipoJander.INTEIRO;
            } else {
                return TabelaDeSimbolos.TipoJander.REAL; // Exemplo de inferência ou padrão
            }
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.CmdAtribuicaoContext ctx) {
        boolean ehDesreferenciacao = ctx.PONTEIRO() != null;
        String nomeVarCompleto = ctx.identificador().getText(); // Isso será "ponto1.x"

        // Usar tabela.existe e tabela.verificar que já lidam com a notação de ponto
        if (!tabela.existe(nomeVarCompleto)) {
            if (adicionarErroSeNecessario(nomeVarCompleto)) {    
                adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVarCompleto + " nao declarado");
            }
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }

        TabelaDeSimbolos.TipoJander tipoVar = tabela.verificar(nomeVarCompleto);
        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
        
        if (tipoExpr == TabelaDeSimbolos.TipoJander.INVALIDO) {
            return tipoVar; // Propagar o tipo da variável, mesmo que a expressão tenha erro
        }

        if (ehDesreferenciacao) {
            // Caso especial para ^ponteiro
            if (tipoVar != TabelaDeSimbolos.TipoJander.PONTEIRO) {
                adicionarErroSemantico(ctx.start, 
                    nomeVarCompleto + " nao e um ponteiro (uso de ^)");
            } else {
                // O tipo real é o tipo base do ponteiro
                TabelaDeSimbolos.TipoJander tipoBase = obterTipoBaseDoPonteiro(tabela, nomeVarCompleto);
                
                // Verificar compatibilidade entre o tipo base e o tipo da expressão
                if (!compatibilidade(tabela, tipoBase, tipoExpr)) {
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para ^" + nomeVarCompleto);
                }
            }
        } else {
            // Verificação especial para ponteiros
            if (tipoVar == TabelaDeSimbolos.TipoJander.PONTEIRO) {
                // Permitir atribuição de endereço (&var) a ponteiro
                if (tipoExpr != TabelaDeSimbolos.TipoJander.PONTEIRO && 
                    tipoExpr != TabelaDeSimbolos.TipoJander.ENDERECO) {
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVarCompleto);
                }
            } 
            // Verificação especial para registros (tipo REGISTRO ou REGISTRO_TIPO)
            else if (tipoVar == TabelaDeSimbolos.TipoJander.REGISTRO || tipoVar == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                if (tipoExpr != tipoVar && tipoExpr != TabelaDeSimbolos.TipoJander.INVALIDO) { // INVALIDO para não adicionar erro duplicado
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para registro " + nomeVarCompleto);
                }
            }
            // Verificação normal para outros tipos
            else if (!compatibilidade(tabela, tipoVar, tipoExpr)) {
                adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVarCompleto);
            }
        }

        return tipoVar;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.CmdChamadaContext ctx) {
        String nomeFunc = ctx.IDENT().getText();
        if (!tabela.existe(nomeFunc)) {
            if (adicionarErroSeNecessario(nomeFunc)) {
                adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeFunc + " nao declarado");
            }
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }
        
        // Verificação de parâmetros
        TabelaDeSimbolos.TipoJander tipoFunc = tabela.verificar(nomeFunc); // This gets the return type if it's a function or its kind (PROCEDURE)
        List<TabelaDeSimbolos.TipoJander> tiposParametros = tabela.obterParametros(nomeFunc);
        
        if (tiposParametros.size() != ctx.expressao().size()) {
            adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
            return tipoFunc; // Return function's declared return type even with parameter mismatch
        }
        
        for (int i = 0; i < tiposParametros.size(); i++) {
            TabelaDeSimbolos.TipoJander tipoParam = tiposParametros.get(i);
            TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao(i));
            
            if (!compatibilidade(tabela, tipoParam, tipoExpr)) {
                adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
                break;
            }
        }
        
        return tipoFunc;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.CmdRetorneContext ctx) {
        // Verifica se estamos dentro de uma função
        if (!tabela.estaEmFuncao()) {
            adicionarErroSemantico(ctx.start, "comando retorne nao permitido nesse escopo");
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }
        
        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
        TabelaDeSimbolos.TipoJander tipoRetorno = tabela.obterTipoRetornoFuncaoAtual();
        
        if (!compatibilidade(tabela, tipoRetorno, tipoExpr)) {
            adicionarErroSemantico(ctx.start, "tipo de retorno incompativel");
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }
        
        return tipoRetorno;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return TabelaDeSimbolos.TipoJander.REGISTRO; // Para registros anônimos
        } else if (ctx.tipo_estendido() != null) {
            TabelaDeSimbolos.TipoJander tipoExt = verificarTipo(tabela, ctx.tipo_estendido());
            // Se o tipo estendido é um IDENT que se refere a um REGISTRO_TIPO
            if (ctx.tipo_estendido().tipo_basico_ident() != null && ctx.tipo_estendido().tipo_basico_ident().IDENT() != null) {
                String nomeIdent = ctx.tipo_estendido().tipo_basico_ident().IDENT().getText();
                if (tabela.existe(nomeIdent) && tabela.verificar(nomeIdent) == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                    return TabelaDeSimbolos.TipoJander.REGISTRO_TIPO; // Retorna o tipo como REGISTRO_TIPO
                }
            }
            return tipoExt;
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }


    // Helper to get types from a parameter context, used during global declaration processing
    public static List<TabelaDeSimbolos.TipoJander> getTypesFromParametroContext(TabelaDeSimbolos tabela, JanderParser.ParametroContext ctx) {
        List<TabelaDeSimbolos.TipoJander> types = new ArrayList<>();
        TabelaDeSimbolos.TipoJander tipoParam = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_estendido());
        for (var ident : ctx.identificador()) {
            types.add(tipoParam);
        }
        return types;
    }
}