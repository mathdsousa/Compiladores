package br.ufscar.dc.compiladores.jander;

public class JanderSemantico extends JanderBaseVisitor<Void> {
    TabelaDeSimbolos tabela;

    @Override
    public Void visitPrograma(JanderParser.ProgramaContext ctx) {
        tabela = new TabelaDeSimbolos();
        return super.visitPrograma(ctx);
    }

    @Override
    public Void visitDeclaracao_local(JanderParser.Declaracao_localContext ctx) {
        if (ctx.variavel() != null) {
            visitVariavel(ctx.variavel());
            return null;
        } else if (ctx.IDENT() != null) {
            // constante ou tipo
            String nomeVar = ctx.IDENT().getText();
            TabelaDeSimbolos.TipoJander tipoVar = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_basico());

            if (tabela.existe(nomeVar)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                    "identificador " + nomeVar + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nomeVar, tipoVar);
                if (tipoVar == TabelaDeSimbolos.TipoJander.INVALIDO) {
                    JanderSemanticoUtils.adicionarErroSemantico(ctx.tipo_basico().start,
                        "tipo " + ctx.tipo_basico().getText() + " nao declarado");
                }
            }
        }
        return null;
    }

    @Override
    public Void visitVariavel(JanderParser.VariavelContext ctx) {
        // Verifica o tipo primeiro
        TabelaDeSimbolos.TipoJander tipoVar = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo());

        for (var ident : ctx.identificador()) {
            String nomeVar = ident.getText();
            if (tabela.existe(nomeVar)) {
                JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                    "identificador " + nomeVar + " ja declarado anteriormente");
            } else {
                if (tipoVar != TabelaDeSimbolos.TipoJander.INVALIDO) {
                    tabela.adicionar(nomeVar, tipoVar);
                } else {
                    // Adiciona o nome da variável à lista de variáveis com erro
                    JanderSemanticoUtils.adicionarErroSeNecessario(nomeVar);
                }
            }
        }

        return super.visitVariavel(ctx);
    }

    @Override
    public Void visitCmdAtribuicao(JanderParser.CmdAtribuicaoContext ctx) {
        JanderSemanticoUtils.verificarTipo(tabela, ctx);
        return null; // NÃO visita os filhos
    }

    @Override
    public Void visitCmdLeia(JanderParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            String nomeVar = ident.getText();
            if (!tabela.existe(nomeVar)) {
                if(JanderSemanticoUtils.adicionarErroSeNecessario(nomeVar)){    
                    JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                        "identificador " + nomeVar + " nao declarado");
                }
            }
        }
        return super.visitCmdLeia(ctx);
    }
    
    @Override
    public Void visitCmdEscreva(JanderParser.CmdEscrevaContext ctx) {
        for (var expr : ctx.expressao()) {
            JanderSemanticoUtils.verificarTipo(tabela, expr);
        }
        return super.visitCmdEscreva(ctx);
    }

    @Override
    public Void visitExpressao(JanderParser.ExpressaoContext ctx) {
        JanderSemanticoUtils.verificarTipo(tabela, ctx);
        return super.visitExpressao(ctx);
    }
    
    @Override
    public Void visitIdentificador(JanderParser.IdentificadorContext ctx) {
        String nome = ctx.getText();
        if (!tabela.existe(nome)) {
            if (JanderSemanticoUtils.adicionarErroSeNecessario(nome)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.start, 
                    "identificador " + nome + " nao declarado");
            }
        }
        return super.visitIdentificador(ctx);
    }
}
