package br.ufscar.dc.compiladores.jander;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import java.util.HashSet;
import java.util.Set;

public class JanderSemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<>();
    private static Set<String> variaveisComErro = new HashSet<>();
    
    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    public static void resetarErros() {
        errosSemanticos.clear();
        variaveisComErro.clear();
    }

    public static boolean compatibilidade(TabelaDeSimbolos tabela, 
                                      TabelaDeSimbolos.TipoJander tipo1, 
                                      TabelaDeSimbolos.TipoJander tipo2) {
        if (tipo1 == tipo2) {
            return true;
        }

        // INTEIRO e REAL são compatíveis entre si
        if ((tipo1 == TabelaDeSimbolos.TipoJander.INTEIRO && tipo2 == TabelaDeSimbolos.TipoJander.REAL) ||
            (tipo1 == TabelaDeSimbolos.TipoJander.REAL && tipo2 == TabelaDeSimbolos.TipoJander.INTEIRO)) {
            return true;
        }

        // LITERAL com qualquer outro tipo é incompatível
        // LOGICO só é compatível com LOGICO (ou REAL, se você quiser forçar isso, mas não parece apropriado)

        return false;
    }

    
    // Função para adicionar o erro semântico e retornar um booleano
    public static boolean adicionarErroSeNecessario(String nome) {
        // Verifica se a variável já foi registrada com erro
        if (!variaveisComErro.contains(nome)) {    
            // Marca a variável como com erro
            variaveisComErro.add(nome);
            return true; // Erro foi adicionado
        }
        // Caso a variável já tenha erro, não adiciona
        return false; // Erro não foi adicionado
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
            // Lógica para ponteiros (se necessário)
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
            if(adicionarErroSeNecessario(tipoNome)){
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

            // Verifica se a variável existe na tabela de símbolos
            if (!tabela.existe(nome)) {
                // Adiciona erro apenas se ainda não foi registrado para esta variável
                if (adicionarErroSeNecessario(nome)) {
                    adicionarErroSemantico(ctx.getStart(), "identificador " + nome + " nao declarado");
                }
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }

            // Se a variável existe, retorna o tipo dela
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
        if (ctx.identificador() != null) {
            String nome = ctx.identificador().getText();
            if (!tabela.existe(nome)) {
                if(adicionarErroSeNecessario(nome)){
                    adicionarErroSemantico(ctx.getStart(), "identificador " + nome + " nao declarado");
                }
                return TabelaDeSimbolos.TipoJander.INVALIDO;
            }
            return tabela.verificar(nome);
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
                    // Só reporta erro se ambos os tipos forem válidos
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

            // Evita erros em cascata
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
            // A comparação retorna um valor lógico
            return TabelaDeSimbolos.TipoJander.LOGICO;
        }

        // Se não há operador relacional, retorna o tipo da expressão aritmética
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

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, JanderParser.CmdAtribuicaoContext ctx) {
        String nomeVar = ctx.identificador().getText();

        // Verifica se a variável existe
        if (!tabela.existe(nomeVar)) {
            if (adicionarErroSeNecessario(nomeVar)) {
                adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVar + " nao declarado");
            }
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }

        TabelaDeSimbolos.TipoJander tipoVar = tabela.verificar(nomeVar);
        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
        
        if (tipoExpr == TabelaDeSimbolos.TipoJander.INVALIDO) {
            adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVar);
            return tipoVar;
        }
        // Se tipos válidos, mas incompatíveis → erro
        if (tipoVar != TabelaDeSimbolos.TipoJander.INVALIDO &&
            tipoExpr != TabelaDeSimbolos.TipoJander.INVALIDO &&
            !compatibilidade(tabela, tipoVar, tipoExpr)) {
            adicionarErroSemantico(ctx.start, "atribuicao nao compativel para " + nomeVar);
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
        return tabela.verificar(nomeFunc);
    }

    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                          JanderParser.CmdRetorneContext ctx) {
        TabelaDeSimbolos.TipoJander tipoExpr = verificarTipo(tabela, ctx.expressao());
        if (tipoExpr == TabelaDeSimbolos.TipoJander.INVALIDO) {
            return TabelaDeSimbolos.TipoJander.INVALIDO;  // Propaga o tipo inválido se houver erro
        }
        return tipoExpr;
    }
    
    public static TabelaDeSimbolos.TipoJander verificarTipo(TabelaDeSimbolos tabela, 
                                                      JanderParser.TipoContext ctx) {
        if (ctx == null) {
            return TabelaDeSimbolos.TipoJander.INVALIDO;
        }

        // Se for um tipo básico
        if (ctx.tipo_estendido() != null) {
            return verificarTipo(tabela, ctx.tipo_estendido());
        }
        return TabelaDeSimbolos.TipoJander.INVALIDO;
    }
}
