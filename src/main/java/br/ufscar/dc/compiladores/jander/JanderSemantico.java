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
        } else if (ctx.IDENT() != null) {
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
        } else if (ctx.tipo() != null) {
            // Declaração de tipo
            String nomeTipo = ctx.IDENT().getText();
            if (tabela.existe(nomeTipo)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                    "identificador " + nomeTipo + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.TipoJander tipo = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo());
                tabela.adicionar(nomeTipo, tipo);
            }
        }
        return null;
    }

    @Override
    public Void visitVariavel(JanderParser.VariavelContext ctx) {
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
                    JanderSemanticoUtils.adicionarErroSeNecessario(nomeVar);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_global(JanderParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();

        if (tabela.existe(nome)) {
            JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                "identificador " + nome + " ja declarado anteriormente");
            return null;
        }

        if (ctx.PROCEDIMENTO() != null) {
            tabela.adicionar(nome, TabelaDeSimbolos.TipoJander.PROCEDIMENTO);
            tabela.novoEscopo(false);

            if (ctx.parametros() != null) {
                visitParametros(ctx.parametros());
            }

            // Corrigido: Visitar declaracoes_locais e comandos separadamente
            for (JanderParser.Declaracao_localContext decl : ctx.declaracao_local()) {
                visitDeclaracao_local(decl);
            }
            for (JanderParser.CmdContext cmd : ctx.cmd()) {
                visitCmd(cmd);
            }

            tabela.abandonarEscopo();
        } else if (ctx.FUNCAO() != null) {
            TabelaDeSimbolos.TipoJander tipoRetorno = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_estendido());
            tabela.adicionar(nome, tipoRetorno);
            tabela.novoEscopo(true);
            tabela.setTipoRetornoFuncaoAtual(tipoRetorno);

            if (ctx.parametros() != null) {
                visitParametros(ctx.parametros());
            }

            // Corrigido: Visitar declaracoes_locais e comandos separadamente
            for (JanderParser.Declaracao_localContext decl : ctx.declaracao_local()) {
                visitDeclaracao_local(decl);
            }
            for (JanderParser.CmdContext cmd : ctx.cmd()) {
                visitCmd(cmd);
            }

            tabela.abandonarEscopo();
        }
        return null;
    }

    @Override
    public Void visitParametro(JanderParser.ParametroContext ctx) {
        TabelaDeSimbolos.TipoJander tipoParam = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_estendido());
        
        for (var ident : ctx.identificador()) {
            String nomeParam = ident.getText();
            if (tabela.existe(nomeParam)) {
                JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                    "identificador " + nomeParam + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nomeParam, tipoParam);
            }
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(JanderParser.CmdAtribuicaoContext ctx) {
        JanderSemanticoUtils.verificarTipo(tabela, ctx);
        return null;
    }

    @Override
    public Void visitCmdLeia(JanderParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            String nomeVar = ident.getText();
            if (!tabela.existe(nomeVar)) {
                if(JanderSemanticoUtils.adicionarErroSeNecessario(nomeVar)) {    
                    JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                        "identificador " + nomeVar + " nao declarado");
                }
            }
        }
        return null;
    }
    
    @Override
    public Void visitCmdEscreva(JanderParser.CmdEscrevaContext ctx) {
        for (var expr : ctx.expressao()) {
            JanderSemanticoUtils.verificarTipo(tabela, expr);
        }
        return null;
    }

    @Override
    public Void visitCmdChamada(JanderParser.CmdChamadaContext ctx) {
        JanderSemanticoUtils.verificarTipo(tabela, ctx);
        return null;
    }

    @Override
    public Void visitCmdRetorne(JanderParser.CmdRetorneContext ctx) {
        JanderSemanticoUtils.verificarTipo(tabela, ctx);
        return null;
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
        return null;
    }
}