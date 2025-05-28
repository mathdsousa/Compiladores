package br.ufscar.dc.compiladores.jander;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        } else if (ctx.CONSTANTE() != null) {
            // Tratamento de constantes
        } else if (ctx.TIPO() != null) {
            String nomeTipo = ctx.IDENT().getText();
            
            if (tabela.existe(nomeTipo)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                    "identificador " + nomeTipo + " ja declarado anteriormente");
            } else {
                // Adiciona o tipo registro na tabela
                // Apenas adicione o nome do tipo, os campos serão adicionados a ele
                tabela.adicionar(nomeTipo, TabelaDeSimbolos.TipoJander.REGISTRO_TIPO); // Mudei para REGISTRO_TIPO

                // Adiciona os campos do tipo registro
                if (ctx.tipo().registro() != null) {
                    for (JanderParser.VariavelContext varCtx : ctx.tipo().registro().variavel()) {
                        TabelaDeSimbolos.TipoJander tipoCampo = JanderSemanticoUtils.verificarTipo(tabela, varCtx.tipo());
                        
                        for (JanderParser.IdentificadorContext idCtx : varCtx.identificador()) {
                            String nomeCampo = idCtx.IDENT(0).getText(); // Pega o primeiro IDENT
                            tabela.adicionarCampoRegistroATipo(nomeTipo, nomeCampo, tipoCampo); // Novo método para adicionar a um tipo
                        }
                    }
                }
            }
        }
        return null;
    }

@Override
public Void visitVariavel(JanderParser.VariavelContext ctx) {
    TabelaDeSimbolos.TipoJander tipoVar = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo());
    String nomeTipoRegistroDeclarado = null; // Para armazenar o nome do tipo de registro se for um TIPO IDENT

    // Se o tipo da variável é um IDENT (ou seja, um tipo de registro nomeado)
    if (ctx.tipo().tipo_estendido() != null && ctx.tipo().tipo_estendido().tipo_basico_ident() != null &&
        ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT() != null) {
        nomeTipoRegistroDeclarado = ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT().getText();
    }
    
    for (var ident : ctx.identificador()) {
        String nomeVar = ident.getText();
        if (tabela.existe(nomeVar)) {
            JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                "identificador " + nomeVar + " ja declarado anteriormente");
        } else {
            tabela.adicionar(nomeVar, tipoVar); // Adiciona a variável à tabela

            // Se for um tipo de registro nomeado (e.g., 'ponto1: MeuTipoDePonto')
            if (nomeTipoRegistroDeclarado != null && tipoVar == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                // Copia os campos do tipo de registro para a instância da variável
                // Certifique-se que o tipo nomeado realmente existe e é um REGISTRO_TIPO
                if (tabela.existe(nomeTipoRegistroDeclarado) && tabela.verificar(nomeTipoRegistroDeclarado) == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                    Map<String, TabelaDeSimbolos.TipoJander> camposDoTipo = tabela.obterCamposDoTipoRegistro(nomeTipoRegistroDeclarado);
                    if (camposDoTipo != null) {
                        for (Map.Entry<String, TabelaDeSimbolos.TipoJander> entry : camposDoTipo.entrySet()) {
                            tabela.adicionarCampoRegistroAInstancia(nomeVar, entry.getKey(), entry.getValue());
                        }
                    }
                }
            } 
            // Se for um registro anônimo declarado diretamente (e.g., 'ponto1: registro ... fim_registro')
            else if (tipoVar == TabelaDeSimbolos.TipoJander.REGISTRO) {
                JanderParser.RegistroContext registroCtx = ctx.tipo().registro();
                if (registroCtx != null) {
                    for (JanderParser.VariavelContext campoVarCtx : registroCtx.variavel()) {
                        TabelaDeSimbolos.TipoJander tipoCampo = JanderSemanticoUtils.verificarTipo(tabela, campoVarCtx.tipo());

                        for (JanderParser.IdentificadorContext campoIdentCtx : campoVarCtx.identificador()) {
                            String nomeCampo = campoIdentCtx.IDENT(0).getText();
                            tabela.adicionarCampoRegistroAInstancia(nomeVar, nomeCampo, tipoCampo);
                        }
                    }
                }
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
            tabela.novoEscopo(false); // New scope for procedure

            List<TabelaDeSimbolos.TipoJander> paramTypes = new ArrayList<>();
            if (ctx.parametros() != null) {
                for (JanderParser.ParametroContext paramCtx : ctx.parametros().parametro()) {
                    paramTypes.addAll(JanderSemanticoUtils.getTypesFromParametroContext(tabela, paramCtx)); 
                }
                visitParametros(ctx.parametros()); // Add parameters to the current (procedure) scope
            }
            tabela.adicionarFuncao(nome, TabelaDeSimbolos.TipoJander.PROCEDIMENTO, paramTypes); // Use adicionarFuncao for procedures too, with PROCEDURE as type and parameters

            for (JanderParser.Declaracao_localContext decl : ctx.declaracao_local()) {
                visitDeclaracao_local(decl);
            }
            for (JanderParser.CmdContext cmd : ctx.cmd()) {
                visitCmd(cmd);
            }

            tabela.abandonarEscopo();
        } else if (ctx.FUNCAO() != null) {
            TabelaDeSimbolos.TipoJander tipoRetorno = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_estendido());
            
            tabela.novoEscopo(true); // New scope for function
            tabela.setTipoRetornoFuncaoAtual(tipoRetorno);
            
            List<TabelaDeSimbolos.TipoJander> paramTypes = new ArrayList<>();
            if (ctx.parametros() != null) {
                for (JanderParser.ParametroContext paramCtx : ctx.parametros().parametro()) {
                    paramTypes.addAll(JanderSemanticoUtils.getTypesFromParametroContext(tabela, paramCtx)); 
                }
                visitParametros(ctx.parametros()); // Add parameters to the current (function) scope
            }
            tabela.adicionarFuncao(nome, tipoRetorno, paramTypes); // Add function with its return type and parameters

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
            String nomeVarCompleto = ident.getText(); // Pode ser "ponto1.x"
            if (!tabela.existe(nomeVarCompleto)) {
                if(JanderSemanticoUtils.adicionarErroSeNecessario(nomeVarCompleto)) {    
                    JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                        "identificador " + nomeVarCompleto + " nao declarado");
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
        // The check for existence of identifiers is primarily handled by the verificarTipo methods
        // in JanderSemanticoUtils when an identifier is used in an expression or command context.
        // This specific visit method might not be strictly necessary for "not declared" errors
        // if all uses of identifiers go through a `verificarTipo` call.
        return null;
    }
}