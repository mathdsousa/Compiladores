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
    
    public static boolean compatibilidade(TabelaDeSimbolos tabela, 
                                      TabelaDeSimbolos.TipoJander tipo1, 
                                      TabelaDeSimbolos.TipoJander tipo2) {
        if (tipo1 == tipo2) {
            return true;
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
        else if (ctx.expressao() != null) {
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
        return TabelaDeSimbolos.TipoJander.INTEIRO; // Assumindo inteiro como padrão para este exemplo
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.CmdAtribuicaoContext ctx) {
        boolean ehDesreferenciacao = ctx.PONTEIRO() != null;
        String nomeVar = ctx.identificador().getText();

        // Verificar se o identificador existe
        if (!tabela.existe(nomeVar)) {
            // Verificar se é um campo de registro (contém ponto)
            if (nomeVar.contains(".")) {
                String[] partes = nomeVar.split("\\.");
                String nomeRegistro = partes[0];
                
                if (tabela.existe(nomeRegistro)) {
                    TabelaDeSimbolos.TipoJander tipoRegistro = tabela.verificar(nomeRegistro);
                    if (tipoRegistro == TabelaDeSimbolos.TipoJander.REGISTRO) {
                        // É um campo de registro válido
                        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
                        
                        // Verificar compatibilidade de tipos para o campo
                        if (tipoExpr == TabelaDeSimbolos.TipoJander.LITERAL) {
                            adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVar);
                        }
                        
                        return TabelaDeSimbolos.TipoJander.REAL; // Assumindo que campos de registro são do tipo real
                    }
                }
            }
            
            if (adicionarErroSeNecessario(nomeVar)) {
                adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVar + " nao declarado");
            }
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }

        TabelaDeSimbolos.TipoJander tipoVar = tabela.verificar(nomeVar);
        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
        
        if (tipoExpr == TabelaDeSimbolos.TipoJander.INVALIDO) {
            return tipoVar;
        }

        if (ehDesreferenciacao) {
            // Caso especial para ^ponteiro
            if (tipoVar != TabelaDeSimbolos.TipoJander.PONTEIRO) {
                adicionarErroSemantico(ctx.start, 
                    nomeVar + " nao e um ponteiro (uso de ^)");
            } else {
                // O tipo real é o tipo base do ponteiro
                TabelaDeSimbolos.TipoJander tipoBase = obterTipoBaseDoPonteiro(tabela, nomeVar);
                
                // Verificar compatibilidade entre o tipo base e o tipo da expressão
                if (!compatibilidade(tabela, tipoBase, tipoExpr)) {
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para ^" + nomeVar);
                }
            }
        } else {
            // Verificação especial para ponteiros
            if (tipoVar == TabelaDeSimbolos.TipoJander.PONTEIRO) {
                // Permitir atribuição de endereço (&var) a ponteiro
                if (tipoExpr != TabelaDeSimbolos.TipoJander.PONTEIRO && 
                    tipoExpr != TabelaDeSimbolos.TipoJander.ENDERECO) {
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVar);
                }
            } 
            // Verificação especial para registros
            else if (tipoVar == TabelaDeSimbolos.TipoJander.REGISTRO) {
                if (tipoExpr != tipoVar) {
                    adicionarErroSemantico(ctx.start, "atribuicao nao compativel para registro " + nomeVar);
                }
            }
            // Verificação normal para outros tipos
            else if (!compatibilidade(tabela, tipoVar, tipoExpr)) {
                adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVar);
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
        TabelaDeSimbolos.TipoJander tipoFunc = tabela.verificar(nomeFunc);
        List<TabelaDeSimbolos.TipoJander> tiposParametros = tabela.obterParametros(nomeFunc);
        
        if (tiposParametros.size() != ctx.expressao().size()) {
            adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeFunc);
            return tipoFunc;
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
            return TabelaDeSimbolos.TipoJander.REGISTRO;
        } else if (ctx.tipo_estendido() != null) {
            return verificarTipo(tabela, ctx.tipo_estendido());
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
}
