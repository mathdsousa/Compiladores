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
            TabelaDeSimbolos.TipoJander tipoVar = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo());
            
            if (tabela.existe(nomeVar)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                    "identificador " + nomeVar + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nomeVar, tipoVar);
            }
        }
        return super.visitDeclaracao_local(ctx);
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
                tabela.adicionar(nomeVar, tipoVar);
            }
        }
        return super.visitVariavel(ctx);
    }

    @Override
    public Void visitCmdAtribuicao(JanderParser.CmdAtribuicaoContext ctx) {
        String nomeVar = ctx.identificador().getText();
        if (!tabela.existe(nomeVar)) {
            JanderSemanticoUtils.adicionarErroSemantico(ctx.identificador().start,
                "identificador " + nomeVar + " nao declarado");
        } else {
            TabelaDeSimbolos.TipoJander tipoVar = tabela.verificar(nomeVar);
            TabelaDeSimbolos.TipoJander tipoExpr = JanderSemanticoUtils.verificarTipo(tabela, ctx.expressao());
            
            if (tipoExpr != TabelaDeSimbolos.TipoJander.INVALIDO && 
                !JanderSemanticoUtils.compatibilidade(tabela, tipoVar, tipoExpr)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.identificador().start,
                    "atribuicao nao compativel para " + nomeVar);
            }
        }
        return super.visitCmdAtribuicao(ctx);
    }

    @Override
    public Void visitCmdLeia(JanderParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            String nomeVar = ident.getText();
            if (!tabela.existe(nomeVar)) {
                JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                    "identificador " + nomeVar + " nao declarado");
            }
        }
        return super.visitCmdLeia(ctx);
    }
}