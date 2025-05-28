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
            String nomeConst = ctx.IDENT().getText();
            TabelaDeSimbolos.TipoJander tipoConst = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_basico());
            Object valorConst = null; //

            // Extrai o valor da constante com base no tipo
            if (ctx.valor_constante().NUM_INT() != null) {
                valorConst = Integer.parseInt(ctx.valor_constante().NUM_INT().getText());
            } else if (ctx.valor_constante().NUM_REAL() != null) {
                valorConst = Double.parseDouble(ctx.valor_constante().NUM_REAL().getText());
            } else if (ctx.valor_constante().CADEIA() != null) {
                // Remove as aspas da string
                String literal = ctx.valor_constante().CADEIA().getText();
                valorConst = literal.substring(1, literal.length() - 1);
            } else if (ctx.valor_constante().VERDADEIRO() != null) {
                valorConst = true;
            } else if (ctx.valor_constante().FALSO() != null) {
                valorConst = false;
            }


            if (tabela.existe(nomeConst)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                    "identificador " + nomeConst + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nomeConst, tipoConst, valorConst); // Adiciona o valor também
            }

        } else if (ctx.TIPO() != null) {
            String nomeTipo = ctx.IDENT().getText();

            if (tabela.existe(nomeTipo)) {
                JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                    "identificador " + nomeTipo + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nomeTipo, TabelaDeSimbolos.TipoJander.REGISTRO_TIPO);

                if (ctx.tipo().registro() != null) {
                    for (JanderParser.VariavelContext varCtx : ctx.tipo().registro().variavel()) {
                        TabelaDeSimbolos.TipoJander tipoCampo = JanderSemanticoUtils.verificarTipo(tabela, varCtx.tipo());

                        for (JanderParser.IdentificadorContext idCtx : varCtx.identificador()) {
                            String nomeCampo = idCtx.IDENT(0).getText();
                            tabela.adicionarCampoRegistroATipo(nomeTipo, nomeCampo, tipoCampo);
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
    String nomeTipoRegistroDeclarado = null;

    if (ctx.tipo().tipo_estendido() != null && ctx.tipo().tipo_estendido().tipo_basico_ident() != null &&
        ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT() != null) {
        nomeTipoRegistroDeclarado = ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT().getText();
    }

    for (var ident : ctx.identificador()) {
        String nomeVar = ident.IDENT(0).getText();

        List<Integer> dimensoes = null;
        if (ident.dimensao() != null && !ident.dimensao().exp_aritmetica().isEmpty()) {
            dimensoes = new ArrayList<>();
            for (JanderParser.Exp_aritmeticaContext dimCtx : ident.dimensao().exp_aritmetica()) {
                // Tenta avaliar a expressão da dimensão. Assumindo que é um literal NUM_INT simples ou um IDENT (constante).
                if (dimCtx.termo().size() == 1 &&
                    dimCtx.termo(0).fator().size() == 1 &&
                    dimCtx.termo(0).fator(0).parcela().size() == 1 &&
                    dimCtx.termo(0).fator(0).parcela(0).parcela_unario() != null) {

                    JanderParser.Parcela_unarioContext parcelaUnarioCtx = dimCtx.termo(0).fator(0).parcela(0).parcela_unario();

                    if (parcelaUnarioCtx.NUM_INT() != null) {
                        try {
                            int dim = Integer.parseInt(parcelaUnarioCtx.NUM_INT().getText());
                            dimensoes.add(dim);
                        } catch (NumberFormatException e) {
                            JanderSemanticoUtils.adicionarErroSemantico(dimCtx.start, "dimensao de array deve ser um inteiro valido");
                            dimensoes.add(0);
                        }
                    } else if (parcelaUnarioCtx.identificador() != null) { // Verifica se é um identificador (constante)
                        String constName = parcelaUnarioCtx.identificador().getText();
                        TabelaDeSimbolos.EntradaTabelaDeSimbolos entradaConst = tabela.getEntrada(constName); //

                        if (entradaConst != null && entradaConst.tipo == TabelaDeSimbolos.TipoJander.INTEIRO && entradaConst.valorConstante instanceof Integer) { //
                            dimensoes.add((Integer) entradaConst.valorConstante); //
                        } else {
                            JanderSemanticoUtils.adicionarErroSemantico(dimCtx.start, "dimensao de array deve ser um literal inteiro ou constante inteira"); //
                            dimensoes.add(0);
                        }
                    } else {
                        JanderSemanticoUtils.adicionarErroSemantico(dimCtx.start, "dimensao de array deve ser um literal inteiro ou constante inteira"); //
                        dimensoes.add(0);
                    }
                } else {
                    JanderSemanticoUtils.adicionarErroSemantico(dimCtx.start, "dimensao de array deve ser um literal inteiro ou constante inteira"); //
                    dimensoes.add(0);
                }
            }
        }

        if (tabela.existe(nomeVar)) {
            JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                "identificador " + nomeVar + " ja declarado anteriormente");
        } else {
            if (dimensoes != null && !dimensoes.isEmpty()) {
                tabela.adicionarArray(nomeVar, tipoVar, dimensoes);
            } else {
                tabela.adicionar(nomeVar, tipoVar);
            }

            if (nomeTipoRegistroDeclarado != null && tabela.existe(nomeTipoRegistroDeclarado) && tabela.verificar(nomeTipoRegistroDeclarado) == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                Map<String, TabelaDeSimbolos.TipoJander> camposDoTipo = tabela.obterCamposDoTipoRegistro(nomeTipoRegistroDeclarado);
                if (camposDoTipo != null) {
                    for (Map.Entry<String, TabelaDeSimbolos.TipoJander> entry : camposDoTipo.entrySet()) {
                        tabela.adicionarCampoRegistroAInstancia(nomeVar, entry.getKey(), entry.getValue());
                    }
                }
            }
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

        //adiciona os tipos de parametros
        List<TabelaDeSimbolos.TipoJander> paramTypes = new ArrayList<>();
        if (ctx.parametros() != null) {
            for (JanderParser.ParametroContext paramCtx : ctx.parametros().parametro()) {
                paramTypes.addAll(JanderSemanticoUtils.getTypesFromParametroContext(tabela, paramCtx));
            }
        }

        if (tabela.existe(nome)) {
            JanderSemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                "identificador " + nome + " ja declarado anteriormente");
            return null;
        }

        //para quando ctx for procedimento
        if (ctx.PROCEDIMENTO() != null) {
            tabela.adicionarFuncao(nome, TabelaDeSimbolos.TipoJander.PROCEDIMENTO, paramTypes);

            tabela.novoEscopo(false);
            if (ctx.parametros() != null) {
                visitParametros(ctx.parametros());
            }

            for (JanderParser.Declaracao_localContext decl : ctx.declaracao_local()) {
                visitDeclaracao_local(decl);
            }
            for (JanderParser.CmdContext cmd : ctx.cmd()) {
                visitCmd(cmd);
            }

            tabela.abandonarEscopo();
        } else if (ctx.FUNCAO() != null) { // para ctx como função
            TabelaDeSimbolos.TipoJander tipoRetorno = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_estendido());

            tabela.adicionarFuncao(nome, tipoRetorno, paramTypes);

            tabela.novoEscopo(true);
            tabela.setTipoRetornoFuncaoAtual(tipoRetorno);

            if (ctx.parametros() != null) {
                visitParametros(ctx.parametros());
            }

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
        // Obtenha o tipo do parâmetro
        TabelaDeSimbolos.TipoJander tipoParam = JanderSemanticoUtils.verificarTipo(tabela, ctx.tipo_estendido());
        String nomeTipoEstendido = null;

        // Se for um tipo estendido e um IDENT (que pode ser um nome de registro)
        if (ctx.tipo_estendido() != null && ctx.tipo_estendido().tipo_basico_ident() != null &&
            ctx.tipo_estendido().tipo_basico_ident().IDENT() != null) {
            nomeTipoEstendido = ctx.tipo_estendido().tipo_basico_ident().IDENT().getText();
        }

        for (var ident : ctx.identificador()) {
            String nomeParam = ident.getText();
            if (tabela.existe(nomeParam)) {
                JanderSemanticoUtils.adicionarErroSemantico(ident.start,
                    "identificador " + nomeParam + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nomeParam, tipoParam);

                // Se o parâmetro for de um tipo REGISTRO_TIPO nomeado, adicione seus campos como campos da instância
                if (nomeTipoEstendido != null && tabela.existe(nomeTipoEstendido) && tabela.verificar(nomeTipoEstendido) == TabelaDeSimbolos.TipoJander.REGISTRO_TIPO) {
                    Map<String, TabelaDeSimbolos.TipoJander> camposDoTipo = tabela.obterCamposDoTipoRegistro(nomeTipoEstendido);
                    if (camposDoTipo != null) {
                        for (Map.Entry<String, TabelaDeSimbolos.TipoJander> entry : camposDoTipo.entrySet()) {
                            tabela.adicionarCampoRegistroAInstancia(nomeParam, entry.getKey(), entry.getValue());
                        }
                    }
                }
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
            String nomeVarCompleto = ident.getText();
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
        JanderSemanticoUtils.verificarCmdRetorne(tabela, ctx);
        return null; 
    }

    @Override
    public Void visitCmdSe(JanderParser.CmdSeContext ctx) {
        TabelaDeSimbolos.TipoJander tipoExpressao = JanderSemanticoUtils.verificarTipo(tabela, ctx.expressao());
        
        if (tipoExpressao != TabelaDeSimbolos.TipoJander.LOGICO && tipoExpressao != TabelaDeSimbolos.TipoJander.INVALIDO) {
            JanderSemanticoUtils.adicionarErroSemantico(ctx.expressao().start, "condicao do se deve ser do tipo logico");
        }

        for (JanderParser.CmdContext cmd : ctx.cmd()) {
            visit(cmd);
        }

        return null; 
    }

    @Override
    public Void visitIdentificador(JanderParser.IdentificadorContext ctx) {
        String nome = ctx.getText();
        return null;
    }
}