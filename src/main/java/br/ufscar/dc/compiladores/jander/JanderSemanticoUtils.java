package br.ufscar.dc.compiladores.jander;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.Token;

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
    
    // Compatibilidade geral para operações (atribuições, expressões aritméticas, etc.)
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

        // Compatibilidade entre INTEIRO e REAL
        if ((tipo1 == TabelaDeSimbolos.TipoJander.INTEIRO && tipo2 == TabelaDeSimbolos.TipoJander.REAL) ||
            (tipo1 == TabelaDeSimbolos.TipoJander.REAL && tipo2 == TabelaDeSimbolos.TipoJander.INTEIRO)) {
            return true;
        }

        return false;
    }

    //compatibilidade de funcao
    public static boolean compatibilidadeFuncao(TabelaDeSimbolos tabela, 
                                  TabelaDeSimbolos.TipoJander tipo1, 
                                  TabelaDeSimbolos.TipoJander tipo2) {
        if (tipo1 == tipo2) {
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
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.Parcela_unarioContext ctx) {
       
        if (ctx.identificador() != null) { // verifica se é identificador
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
        else if (ctx.expressao() != null && !ctx.expressao().isEmpty() && ctx.IDENT() == null) { // é expressão
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
        }else if (ctx.IDENT() != null && ctx.ABREPAR() != null && ctx.FECHAPAR() != null) { // funções 
            String nomeFunc = ctx.IDENT().getText();
            if (!tabela.existe(nomeFunc)) {
                if (adicionarErroSeNecessario(nomeFunc)) {
                    adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeFunc + " nao declarado");
                }
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }

            TabelaDeSimbolos.TipoJander tipoNoTabela = tabela.verificar(nomeFunc);

            if (tipoNoTabela != TabelaDeSimbolos.TipoJander.FUNCAO && tipoNoTabela != TabelaDeSimbolos.TipoJander.PROCEDIMENTO) {
                adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeFunc + " nao e uma funcao ou procedimento");
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            
            List<TabelaDeSimbolos.TipoJander> tiposParametrosEsperados = tabela.obterParametros(nomeFunc);
            List<JanderParser.ExpressaoContext> argumentosPassados = ctx.expressao();

            if (tiposParametrosEsperados.size() != argumentosPassados.size()) {
                adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
            } else {
                for (int i = 0; i < tiposParametrosEsperados.size(); i++) {
                    TabelaDeSimbolos.TipoJander tipoParamEsperado = tiposParametrosEsperados.get(i);
                    TabelaDeSimbolos.TipoJander tipoArgumentoPassado = verificarTipo(tabela, argumentosPassados.get(i));
                
                    if (!compatibilidadeFuncao(tabela, tipoParamEsperado, tipoArgumentoPassado)) { 
                        adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
                        break;
                    }
                }
            }
            return (tipoNoTabela == TabelaDeSimbolos.TipoJander.FUNCAO) ? tabela.obterTipoRetorno(nomeFunc) : TabelaDeSimbolos.TipoJander.INVALIDO;
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
    
    // Aplicada em Fator, Termo e Exp_aritmetica
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                         JanderParser.FatorContext ctx) {
        TabelaDeSimbolos.TipoJander tipoResultante = null;
        for (var parcela : ctx.parcela()) {
            TabelaDeSimbolos.TipoJander tipoParcela = verificarTipo(tabela, parcela);

            if (tipoParcela == TabelaDeSimbolos.TipoJander.INVALIDO) {
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }

            if (tipoResultante == null) {
                tipoResultante = tipoParcela;
            } else {
                if (tipoResultante == TabelaDeSimbolos.TipoJander.REAL || tipoParcela == TabelaDeSimbolos.TipoJander.REAL) {
                    if ((tipoResultante == TabelaDeSimbolos.TipoJander.INTEIRO || tipoResultante == TabelaDeSimbolos.TipoJander.REAL) &&
                        (tipoParcela == TabelaDeSimbolos.TipoJander.INTEIRO || tipoParcela == TabelaDeSimbolos.TipoJander.REAL)) {
                        tipoResultante = TabelaDeSimbolos.TipoJander.REAL;
                    } else {
                        adicionarErroSemantico(ctx.start, "fator contem tipos incompativeis para operacao aritmetica");
                        return TabelaDeSimbolos.TipoJander.INVALIDO;
                    }
                } else if (tipoResultante == TabelaDeSimbolos.TipoJander.INTEIRO && tipoParcela == TabelaDeSimbolos.TipoJander.INTEIRO) {
                    tipoResultante = TabelaDeSimbolos.TipoJander.INTEIRO;
                } else {
                    if (!compatibilidade(tabela, tipoResultante, tipoParcela)) {
                        adicionarErroSemantico(ctx.start, "fator contem tipos incompativeis para operacao aritmetica");
                        return TabelaDeSimbolos.TipoJander.INVALIDO;
                    }
                }
            }
        }
        return tipoResultante;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                     JanderParser.TermoContext ctx) {
        TabelaDeSimbolos.TipoJander tipoResultante = null;
        for (var fator : ctx.fator()) {
            TabelaDeSimbolos.TipoJander tipoFator = verificarTipo(tabela, fator);

            if (tipoFator == TabelaDeSimbolos.TipoJander.INVALIDO) {
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }

            if (tipoResultante == null) {
                tipoResultante = tipoFator;
            } else {
                if (tipoResultante == TabelaDeSimbolos.TipoJander.REAL || tipoFator == TabelaDeSimbolos.TipoJander.REAL) {
                    if ((tipoResultante == TabelaDeSimbolos.TipoJander.INTEIRO || tipoResultante == TabelaDeSimbolos.TipoJander.REAL) &&
                        (tipoFator == TabelaDeSimbolos.TipoJander.INTEIRO || tipoFator == TabelaDeSimbolos.TipoJander.REAL)) {
                        tipoResultante = TabelaDeSimbolos.TipoJander.REAL;
                    } else {
                        adicionarErroSemantico(ctx.start, "termo contem tipos incompativeis para operacao aritmetica");
                        return TabelaDeSimbolos.TipoJander.INVALIDO;
                    }
                } else if (tipoResultante == TabelaDeSimbolos.TipoJander.INTEIRO && tipoFator == TabelaDeSimbolos.TipoJander.INTEIRO) {
                    tipoResultante = TabelaDeSimbolos.TipoJander.INTEIRO;
                } else {
                    if (!compatibilidade(tabela, tipoResultante, tipoFator)) {
                        adicionarErroSemantico(ctx.start, "termo contem tipos incompativeis para operacao aritmetica");
                        return TabelaDeSimbolos.TipoJander.INVALIDO;
                    }
                }
            }
        }
        return tipoResultante != null ? tipoResultante : TabelaDeSimbolos.TipoJander.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                  JanderParser.Exp_aritmeticaContext ctx) {
        TabelaDeSimbolos.TipoJander tipoResultante = null;
        for (var termo : ctx.termo()) {
            TabelaDeSimbolos.TipoJander tipoTermo = verificarTipo(tabela, termo);

            if (tipoTermo == TabelaDeSimbolos.TipoJander.INVALIDO) {
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }

            if (tipoResultante == null) {
                tipoResultante = tipoTermo;
            } else {
                if (tipoResultante == TabelaDeSimbolos.TipoJander.REAL || tipoTermo == TabelaDeSimbolos.TipoJander.REAL) {
                    if ((tipoResultante == TabelaDeSimbolos.TipoJander.INTEIRO || tipoResultante == TabelaDeSimbolos.TipoJander.REAL) &&
                        (tipoTermo == TabelaDeSimbolos.TipoJander.INTEIRO || tipoTermo == TabelaDeSimbolos.TipoJander.REAL)) {
                        tipoResultante = TabelaDeSimbolos.TipoJander.REAL;
                    } else {
                        adicionarErroSemantico(ctx.start, "expressao aritmetica contem tipos incompativeis");
                        return TabelaDeSimbolos.TipoJander.INVALIDO;
                    }
                } else if (tipoResultante == TabelaDeSimbolos.TipoJander.INTEIRO && tipoTermo == TabelaDeSimbolos.TipoJander.INTEIRO) {
                    tipoResultante = TabelaDeSimbolos.TipoJander.INTEIRO;
                } else {
                    if (!compatibilidade(tabela, tipoResultante, tipoTermo)) {
                        adicionarErroSemantico(ctx.start, "expressao aritmetica contem tipos incompativeis");
                        return TabelaDeSimbolos.TipoJander.INVALIDO;
                    }
                }
            }
        }
        return tipoResultante != null ? tipoResultante : TabelaDeSimbolos.TipoJander.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                      JanderParser.Exp_relacionalContext ctx) {
        TabelaDeSimbolos.TipoJander tipo1 = verificarTipo(tabela, ctx.exp_aritmetica(0));

        if (ctx.op_relacional() != null) {
            TabelaDeSimbolos.TipoJander tipo2 = verificarTipo(tabela, ctx.exp_aritmetica(1));
            if (!compatibilidade(tabela, tipo1, tipo2)) {
                adicionarErroSemantico(ctx.start, "Operacao relacional com tipos incompativeis");
                return TabelaDeSimbolos.TipoJander.LOGICO; // Still return LOGICO even if error, to allow further checks
            }
            return TabelaDeSimbolos.TipoJander.LOGICO;
        }

        return tipo1;
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
        TabelaDeSimbolos.TipoJander tipoDoPonteiro = tabela.verificar(nomePonteiro);
        if (tipoDoPonteiro == TabelaDeSimbolos.TipoJander.PONTEIRO) {
            if (nomePonteiro.toLowerCase().contains("int")) {
                return TabelaDeSimbolos.TipoJander.INTEIRO;
            } else {
                return TabelaDeSimbolos.TipoJander.REAL; 
            }
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.CmdAtribuicaoContext ctx) {
        boolean ehDesreferenciacao = ctx.PONTEIRO() != null;
        String nomeVarCompleto = ctx.identificador().getText(); 

        if (!tabela.existe(nomeVarCompleto)) {
            if (adicionarErroSeNecessario(nomeVarCompleto)) {    
                adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVarCompleto + " nao declarado");
            }
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }

        TabelaDeSimbolos.TipoJander tipoVar = tabela.verificar(nomeVarCompleto);
        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
        
        if (tipoExpr == TabelaDeSimbolos.TipoJander.INVALIDO) {
            return tipoVar; 
        }

        if (ehDesreferenciacao) {
            if (tipoVar != TabelaDeSimbolos.TipoJander.PONTEIRO) {
                adicionarErroSemantico(ctx.start, 
                    nomeVarCompleto + " nao e um ponteiro (uso de ^)");
            } else {
                TabelaDeSimbolos.TipoJander tipoBase = obterTipoBaseDoPonteiro(tabela, nomeVarCompleto);
                
                if (!compatibilidade(tabela, tipoBase, tipoExpr)) {
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para ^" + nomeVarCompleto);
                }
            }
        } else {
            if (tipoVar == TabelaDeSimbolos.TipoJander.PONTEIRO) {
                if (tipoExpr != TabelaDeSimbolos.TipoJander.PONTEIRO && 
                    tipoExpr != TabelaDeSimbolos.TipoJander.ENDERECO) {
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVarCompleto);
                }
            } 
            else if (tipoVar == TabelaDeSimbolos.TipoJander.REGISTRO || tipoVar == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                if (tipoExpr != tipoVar && tipoExpr != TabelaDeSimbolos.TipoJander.INVALIDO) { 
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para registro " + nomeVarCompleto);
                }
            }
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
        
        TabelaDeSimbolos.TipoJander tipoFunc = tabela.verificar(nomeFunc); 
        List<TabelaDeSimbolos.TipoJander> tiposParametros = tabela.obterParametros(nomeFunc);
        
        if (tiposParametros.size() != ctx.expressao().size()) {
            adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
            return tipoFunc; 
        }
        
        for (int i = 0; i < tiposParametros.size(); i++) {
            TabelaDeSimbolos.TipoJander tipoParam = tiposParametros.get(i);
            TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao(i));
            if (!compatibilidadeFuncao(tabela, tipoParam, tipoExpr)) {
                adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
                break;
            }
        }
        
        return tipoFunc;
    }

    public static void verificarCmdRetorne(TabelaDeSimbolos tabela, JanderParser.CmdRetorneContext ctx) {
        if (!tabela.estaEmFuncao()) {
            adicionarErroSemantico(ctx.start, "comando retorne nao permitido nesse escopo");
            return;
        }
        
        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
        TabelaDeSimbolos.TipoJander tipoRetornoEsperado = tabela.obterTipoRetornoFuncaoAtual();
        
        if (!compatibilidade(tabela, tipoRetornoEsperado, tipoExpr)) {
            adicionarErroSemantico(ctx.start, "tipo de retorno incompativel");
        }
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return TabelaDeSimbolos.TipoJander.REGISTRO; 
        } else if (ctx.tipo_estendido() != null) {
            TabelaDeSimbolos.TipoJander tipoExt = verificarTipo(tabela, ctx.tipo_estendido());
            if (ctx.tipo_estendido().tipo_basico_ident() != null && ctx.tipo_estendido().tipo_basico_ident().IDENT() != null) {
                String nomeIdent = ctx.tipo_estendido().tipo_basico_ident().IDENT().getText();
                if (tabela.existe(nomeIdent) && tabela.verificar(nomeIdent) == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                    return TabelaDeSimbolos.TipoJander.REGISTRO_TIPO; 
                }
            }
            return tipoExt;
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }

    public static List<TabelaDeSimbolos.TipoJander> getTypesFromParametroContext(TabelaDeSimbolos tabela, JanderParser.ParametroContext ctx) {
        List<TabelaDeSimbolos.TipoJander> types = new ArrayList<>();
        TabelaDeSimbolos.TipoJander tipoParam = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_estendido());
        for (var ident : ctx.identificador()) {
            types.add(tipoParam);
        }
        return types;
    }
}