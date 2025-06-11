package br.ufscar.dc.compiladores.jander;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static br.ufscar.dc.compiladores.jander.JanderSemanticoUtils.verificarTipo;
import br.ufscar.dc.compiladores.jander.TabelaDeSimbolos.TipoJander;

public class Gerador extends JanderBaseVisitor<Void> {

    // String que receberá o programa em C ao longo da análise.
    StringBuilder saida  = new StringBuilder();
    
    // Criação da tabela principal e inicialização de escopos.
    TabelaDeSimbolos tabela  = new TabelaDeSimbolos();
    
    // Método auxiliar que recebe um TipoJander e o converte para a
    // String equivalente de um tipo em C.
    public String converteTipo(TipoJander tipoAuxTipoJander) {
        
        String tipoRetorno = null;
        
        if (tipoAuxTipoJander != null) { 
            switch (tipoAuxTipoJander) {
                case INTEIRO:
                    tipoRetorno = "int";
                    break; 
                case LITERAL:
                    tipoRetorno = "char";
                    break; 
                case REAL:
                    tipoRetorno = "float";
                    break; 
                default:
                    break;
            }
        }
        
        return tipoRetorno;
    }

    // Método auxiliar que recebe uma string de um tipo dos casos
    // de entrada e retorna o TipoJander auxiliar.
    public TipoJander converteTipoJander(String tipo) {

        TipoJander tipoRetorno = TipoJander.INVALIDO;

        switch (tipo) {
            case "literal":
                tipoRetorno = TipoJander.LITERAL;
                break;
            case "inteiro":
                tipoRetorno = TipoJander.INTEIRO;
                break;
            case "real":
                tipoRetorno = TipoJander.REAL;
                break;
            case "logico":
                tipoRetorno = TipoJander.LOGICO;
                break;
            default:
                break;
        }

        return tipoRetorno;
    }

    // Método auxiliar que recebe uma string com um tipo e retorna
    // o tipo C equivalente.
    public String verificaTipoC(String tipo) {
        
        String tipoRetorno = null;
        
        switch (tipo) {
            case "inteiro":
                tipoRetorno = "int";
                break; 
            case "literal":
                tipoRetorno = "char";
                break; 
            case "real":
                tipoRetorno = "float";
                break; 
            default:
                break;
        }
        
        return tipoRetorno;
    }

    // Método auxiiliar que recebe uma string que representa um tipo C
    // e retorna o parâmetro equivalente para sua leitura ou impressão em C.
    public String verificaParamTipo(String tipo) {
        
        String tipoRetorno = null;

        switch (tipo) {
            case "int":
                tipoRetorno = "d";
                break; 
            case "float":
                tipoRetorno = "f";
                break; 
            case "char":
                tipoRetorno = "s";
                break; 
            default:
                break;
        }

        return tipoRetorno;
    }

    // Método auxiiliar que recebe um TipoJander e retorna o parâmetro 
    // equivalente para sua leitura ou impressão em C.  
    public String verificaParamTipoJander(TipoJander tipoAuxTipoJander) {

        String tipoRetorno = null;
        if (tipoAuxTipoJander != null) {
            switch (tipoAuxTipoJander) {
                case INTEIRO:
                    tipoRetorno = "d";
                    break;
                case REAL:
                    tipoRetorno = "f";
                    break;
                case LITERAL:
                    tipoRetorno = "s";
                    break;
                default:
                    break;
            }
        }
        
        return tipoRetorno;
    }

    // Método auxiliar que verifica a existência de um tipo na tabela.
    public boolean verificaTipoTabela(TabelaDeSimbolos tabela, String tipo){

        return tabela.existe(tipo);
    }
    
    // Método auxiliar que recebe uma string e retorna os limites utilizados
    // em um comando Caso (Switch).
    public String getLimitesCaso(String str, boolean ehEsquerda) {
                
        String strAux;
        
        // Verificação necessária para identificar se trata-se de uma expressão
        // com limites esquerdo e direito, ou se é apenas um único valor.
        if (str.contains(".")) {

            int cont = 0;
            boolean continua = true;
            
            while (continua) {
                strAux = str.substring(cont);

                if (strAux.startsWith("."))
                    continua = false;
                else
                    cont++;
            }

            if (ehEsquerda)
                strAux = str.substring(0, cont);
            else
                strAux = str.substring(cont + 2);
        
        } else
            strAux = str;

        return strAux;
    }
    
    // Método auxiliar que separa os argumentos de um expressão relacional,
    // usado nos casos dos operadores de igual e diferente.
    public String separaArg(String total, int valor) {
        
        String argAux;
        boolean continua = true;
        int cont = 0;

        total = total.substring(1);
        
        while (continua) {
            argAux = total.substring(cont);

            if (argAux.startsWith("=") || argAux.startsWith("<>"))
                continua = false;
            else
                cont++;
        }

        if (valor == 0) {
            argAux = total.substring(0, cont);
        } else {
            total = total.substring(cont+1);
            cont = 0;
            continua = true;
            while (continua) {
                argAux = total.substring(cont);

                if (argAux.startsWith(")"))
                    continua = false;
                else
                    cont++;
            }
            argAux = total.substring(0, cont);
        }
        
        return argAux;
    }
    
    // Método auxiliar que recebe uma string e separa as parcelas de
    // uma expressão aritmética.
    public String separaExp(String total, int valor) {
        
        String argAux;
        boolean continua = true;
        int cont = 0;
        
        while (continua) {
            argAux = total.substring(cont);

            if (argAux.startsWith("+") || argAux.startsWith("-") || argAux.startsWith("*") || argAux.startsWith("/"))
                continua = false;
            else
                cont++;
        }

        if (valor == 0)
            argAux = total.substring(0, cont);
        else
            argAux = total.substring(cont + 1);

        return argAux;
    }
    
    // Método auxiliar que recebe uma string de uma expressão e retorna
    // apenas o operador utilizado na expressão.
    public String verificaOp(String total) {
        String opRetorno = null;
        
        if (total.contains("+"))
            opRetorno = "+";
        else if (total.contains("-"))
            opRetorno = "-";
        else if (total.contains("*"))
            opRetorno = "*";
        else if (total.contains("/"))
            opRetorno = "/";

        return opRetorno;
    }
    
    @Override
    public Void visitPrograma(JanderParser.ProgramaContext ctx) {
        
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("#include <string.h>\n");
        saida.append("\n");
         
        visitDeclaracoes(ctx.declaracoes());

        saida.append("\nint main() {\n"); 

        visitCorpo(ctx.corpo());

        saida.append("\nreturn 0;\n");
        saida.append("}\n");

        return null;
    }
    
    @Override
public Void visitDeclaracao_local(JanderParser.Declaracao_localContext ctx) {
    String str;

    if (ctx.valor_constante() != null) {
        str = "#define " + ctx.IDENT().getText() + " " + ctx.valor_constante().getText() + "\n";
        saida.append(str);
    } 
    else if (ctx.tipo() != null) {
        // 1. Primeiro adiciona o tipo de registro no escopo GLOBAL
        String nomeRegistro = ctx.IDENT().getText();
        tabela.adicionar(nomeRegistro, TabelaDeSimbolos.TipoJander.REGISTRO_TIPO);
        
        // 2. Gera a struct
        saida.append("typedef struct {\n");
        
        // 3. Processa os campos (sem criar novo escopo)
        if (ctx.tipo().registro() != null) {
            for (JanderParser.VariavelContext varCtx : ctx.tipo().registro().variavel()) {
                TabelaDeSimbolos.TipoJander tipoCampo = JanderSemanticoUtils.verificarTipo(tabela, varCtx.tipo());
                
                for (JanderParser.IdentificadorContext idCtx : varCtx.identificador()) {
                    String nomeCampo = idCtx.IDENT(0).getText();
                    
                    // Gera o código do campo
                    saida.append("    " + converteTipo(tipoCampo) + " " + nomeCampo);
                    if (tipoCampo == TipoJander.LITERAL) {
                        saida.append("[80]");
                    }
                    saida.append(";\n");
                    
                    // Adiciona o campo ao tipo de registro
                    tabela.adicionarCampoRegistroATipo(nomeRegistro, nomeCampo, tipoCampo);
                }
            }
        }
        
        saida.append("} ").append(nomeRegistro).append(";\n");
    }
    else if (ctx.variavel() != null) {
        visitVariavel(ctx.variavel());
    }
    
    return null;
}
    @Override
    public Void visitVariavel(JanderParser.VariavelContext ctx) {

        boolean tipoEstendido = false;
        String str;

        // Verifica se é um tipo básico.
        if (ctx.tipo().tipo_estendido() != null) {
            String nomeVar;
            String tipoVariavel = ctx.tipo().getText();
            TipoJander tipoAuxTipoJander;
            boolean ehPonteiro = false;

            if (tipoVariavel.contains("^")) {
                ehPonteiro = true;
                tipoVariavel = tipoVariavel.substring(1);
            }

            if (tabela.existe(tipoVariavel) && tabela.verificar(tipoVariavel) == TipoJander.REGISTRO_TIPO) {
                // É um tipo registro definido anteriormente
                for (JanderParser.IdentificadorContext ictx : ctx.identificador()) {
                    nomeVar = ictx.getText();
                    tabela.adicionar(nomeVar, TipoJander.REGISTRO_TIPO);
                    
                    // Gera a declaração usando o tipo definido
                    str = tipoVariavel + " " + nomeVar + ";\n";
                    saida.append(str);
                }
                return null;
            } else {
                // É um tipo básico
                tipoAuxTipoJander = converteTipoJander(tipoVariavel);
                tipoVariavel = converteTipo(tipoAuxTipoJander);
            }

            if (ehPonteiro) {
                tipoVariavel += "*";
            }

            for (JanderParser.IdentificadorContext ictx : ctx.identificador()) {
                nomeVar = ictx.getText();

                tabela.adicionar(nomeVar, tipoAuxTipoJander);

                if (tipoAuxTipoJander == TipoJander.LITERAL) {
                    saida.append(tipoVariavel).append(" ").append(nomeVar).append("[80];\n");
                } else {
                    saida.append(tipoVariavel).append(" ").append(nomeVar).append(";\n");
                }
            }

        } else {
            // Cria um novo escopo temporário para o corpo do registro
            tabela.novoEscopo(false);

            saida.append(" struct {\n");

            super.visitRegistro(ctx.tipo().registro());

            tabela.abandonarEscopo();

            String nomeRegistro = ctx.identificador(0).getText();
            tabela.adicionar(nomeRegistro, TabelaDeSimbolos.TipoJander.REGISTRO_TIPO);

                if (ctx.tipo().registro() != null) {
                    for (JanderParser.VariavelContext varCtx : ctx.tipo().registro().variavel()) {
                        TabelaDeSimbolos.TipoJander tipoCampo = JanderSemanticoUtils.verificarTipo(tabela, varCtx.tipo());

                        for (JanderParser.IdentificadorContext idCtx : varCtx.identificador()) {
                            String nomeCampo = idCtx.IDENT(0).getText();
                            tabela.adicionarCampoRegistroATipo(nomeRegistro, nomeCampo, tipoCampo);
                        }
                    }
                }

            saida.append("} ").append(nomeRegistro).append(";\n");
        }


        return null;
    }
    
    @Override
    public Void visitDeclaracao_global(JanderParser.Declaracao_globalContext ctx) {
        String str;

        // Cria um novo escopo para os parâmetros e corpo da função/procedimento
        tabela.novoEscopo(true);

        String tipo, nomeVariaveis;

        // Verifica e imprime o tipo da função em C.
        if (ctx.FUNCAO() != null)
            saida.append(verificaTipoC(ctx.tipo_estendido().getText()));
        else
            saida.append("void");

        // Imprime o nome da função/procedimento.
        str = " " + ctx.IDENT().getText() + "(";
        saida.append(str);

        // Adiciona os parâmetros no escopo atual e os imprime
        if (ctx.parametros() != null) {
            List<TabelaDeSimbolos.TipoJander> tiposParametros = new ArrayList<>();
            boolean primeiro = true;

            for (JanderParser.ParametroContext pctx : ctx.parametros().parametro()) {
                tipo = verificaTipoC(pctx.tipo_estendido().getText());
                TipoJander tipoParam = converteTipoJander(pctx.tipo_estendido().getText());

                for (JanderParser.IdentificadorContext ictx : pctx.identificador()) {
                    String nomeVar = ictx.getText();
                    tabela.adicionar(nomeVar, tipoParam);
                    tiposParametros.add(tipoParam);

                    if (!primeiro) {
                        saida.append(", ");
                    }
                    primeiro = false;

                    // Ajuste de tipo literal para strings
                    if (tipo.equals("char")) {
                        tipo = "char*";
                    }

                    saida.append(tipo).append(" ").append(nomeVar);
                }
            }

            // Registra a função na tabela com os parâmetros
            if (ctx.tipo_estendido() != null) {
                tabela.adicionarFuncao(ctx.IDENT().getText(), converteTipoJander(ctx.tipo_estendido().getText()), tiposParametros);
            } else {
                tabela.adicionarFuncao(ctx.IDENT().getText(), TipoJander.INVALIDO, tiposParametros);
            }
        } else {
            // Procedimento sem parâmetros
            if (ctx.tipo_estendido() != null) {
                tabela.adicionarFuncao(ctx.IDENT().getText(), converteTipoJander(ctx.tipo_estendido().getText()), new ArrayList<>());
            } else {
                tabela.adicionarFuncao(ctx.IDENT().getText(), TipoJander.INVALIDO, new ArrayList<>());
            }
        }

        saida.append(") {\n");

        // Visita comandos do corpo
        for (JanderParser.CmdContext cctx : ctx.cmd()) {
            visitCmd(cctx);
        }

        saida.append("}\n");

        tabela.abandonarEscopo(); // Encerra o escopo da função

        return null;
    }

    
    @Override
    public Void visitParcela_nao_unario(JanderParser.Parcela_nao_unarioContext ctx) {

        // Imprime o identificador da parcela.
        if (ctx.identificador() != null)
            saida.append(ctx.identificador().getText());

        super.visitParcela_nao_unario(ctx);

        return null;
    }
    
    @Override
    public Void visitParcela_unario(JanderParser.Parcela_unarioContext ctx) {
        
        // Verifica se é uma expressão sem parênteses.
        if (!ctx.expressao().get(0).getText().contains("(")) {
            saida.append(ctx.getText());
        // Caso a expressão tenha parênteses, analisa as expressões internas de forma
        // individual e recursiva.
        } else {
            saida.append("(");
            super.visitParcela_unario(ctx);
            saida.append(")");
        }
        
        return null;
    }
    
    @Override
    public Void visitOp_relacional(JanderParser.Op_relacionalContext ctx) {

        // String criada para receber o operador da expressão atual.
        String strRetorno = ctx.getText();

        // Verifica se é o operador de igual para substituí-lo pelo operador
        // correto em C.
        if (ctx.getText().contains("="))
            if (!ctx.getText().contains("<=") || !ctx.getText().contains(">="))
                strRetorno = "==";

        saida.append(strRetorno);

        super.visitOp_relacional(ctx);

        return null;
    }
    
    @Override
    public Void visitCmdRetorne(JanderParser.CmdRetorneContext ctx) {

        saida.append("return ");
        // Análise da expressão que está sendo retornada.
        super.visitExpressao(ctx.expressao());
        saida.append(";\n");

        return null;
    }
    
    @Override
    public Void visitCmdAtribuicao(JanderParser.CmdAtribuicaoContext ctx) {
        String str;

        // Obtém o escopo atual a partir da pilha
        TabelaDeSimbolos escopoAtual = tabela;

        String nomeIdent = ctx.identificador().getText();
        String nomeBase = nomeIdent.split("\\.")[0]; // Caso seja acesso a campo de registro

        // Verifica se o identificador existe no escopo (com ou sem ponto)
        if (!escopoAtual.existe(nomeBase)) {
            System.err.println("Erro: variável '" + nomeBase + "' não declarada.");
        }

        // Ponteiro
        if (ctx.getText().contains("^")) {
            str = "*" + nomeIdent + " = " + ctx.expressao().getText() + ";\n";
            saida.append(str);

        // Atribuição de string a campo de registro
        } else if (nomeIdent.contains(".") && ctx.getText().contains("\"")) {
            str = "strcpy(" + nomeIdent + ", " + ctx.expressao().getText() + ");\n";
            saida.append(str);

        // Atribuição comum
        } else {
            str = nomeIdent + " = " + ctx.expressao().getText() + ";\n";
            saida.append(str);
        }

        return null;
    }

    
    @Override
    public Void visitExpressao(JanderParser.ExpressaoContext ctx) {

        // Verifica se existe mais de um argumento no termo e, caso
        // não exista, executa a visitação apenas no primeiro e único termo.
        if (ctx.termo_logico().size() > 1) {
        
            for(JanderParser.Termo_logicoContext termoLogico : ctx.termo_logico()) {
                saida.append(" || ");
                visitTermo_logico(termoLogico);
            }
        
        } else
            visitTermo_logico(ctx.termo_logico(0));

        return null;
    }
    
    @Override 
    public Void visitTermo_logico(JanderParser.Termo_logicoContext ctx){
        
        // Verifica se existe mais de um argumento no fator e, caso
        // não exista, executa a visitação apenas no primeiro e único fator.
        if (ctx.fator_logico().size() > 1) {
        
            for(JanderParser.Fator_logicoContext fatorLogico : ctx.fator_logico()) {
                saida.append(" && ");
                visitFator_logico(fatorLogico);
            }
        } else
            visitFator_logico(ctx.fator_logico(0));

        return null;
        
    }
    
    @Override
    public Void visitFator_logico(JanderParser.Fator_logicoContext ctx) {
        
        // Verifica a existência do operador de negação.
        if(ctx.getText().contains("nao"))
            saida.append("!");

        visitParcela_logica(ctx.parcela_logica());
        
        return null;
    }
    
    @Override
    public Void visitOp2(JanderParser.Op2Context ctx) {

        saida.append(ctx.getText());

        super.visitOp2(ctx);

        return null;
    }
    
    @Override
    public Void visitParcela_logica(JanderParser.Parcela_logicaContext ctx) {
        
        if(ctx.getText().contains("falso"))
            saida.append("false");
        else if(ctx.getText().contains("verdadeiro"))
            saida.append("true");
        else
            visitExp_relacional(ctx.exp_relacional());

        return null;
    }
    
    @Override
    public Void visitExp_relacional(JanderParser.Exp_relacionalContext ctx) {
        
        String str;
        String opAtual = ctx.getText();
        String expAtual = ctx.exp_aritmetica().get(0).getText();

        // Verifica se o operador atual é o de diferença ou igualdade, para tratá-los
        // de forma especial visto que precisam ser substituídos por seus equivalentes
        // em C.
        if (expAtual.contains("<>"))
            opAtual = "<>";
        else if (expAtual.contains("="))
            if (!expAtual.contains("<=") || !expAtual.contains(">="))
                opAtual = "=";

        if (ctx.op_relacional() != null) {
            saida.append(expAtual);
            saida.append(ctx.op_relacional().getText());
            saida.append(ctx.exp_aritmetica(1).getText());
        // Situação em que precisa tratar dos operadores de igualdade e diferença,
        // ou de uma expressão aritmética.
        } else {
            switch(opAtual) {
                case "=" :
                    String arg1, arg2;
                    arg1 = separaArg(expAtual, 0);
                    arg2 = separaArg(expAtual, 1);
                    str = "(" + arg1;
                    saida.append(str);
                    saida.append("==");
                    str = arg2 + ")";
                    saida.append(str);
                    break;
                case "<>":
                    saida.append("!=");
                    break;
                // Caso não seja nenhum dos casos anteriores, trata-se de uma
                // expressão aritmética.
                default:
                    arg1 = separaExp(expAtual, 0);
                    arg2 = separaExp(expAtual, 1);
                    saida.append(arg1);
                    String op = verificaOp(opAtual);
                    saida.append(op);
                    saida.append(arg2);
                    break;
            }
        }

        return null;
    }
    
    @Override
    public Void visitCmdSe(JanderParser.CmdSeContext ctx) {

        String str;
        String textoExpressao;
        
        // Susbtitui os operadores nas expressões da condição.
        textoExpressao = ctx.expressao().getText().replace("e", "&&");
        textoExpressao = textoExpressao.replace("=", "==");

        str = "if (" + textoExpressao + "){\n";
        saida.append(str);

        // Realiza os comandos do if.
        for (JanderParser.CmdContext cctx : ctx.cmdEntao)
            super.visitCmd(cctx);

        saida.append("}\n");

        // Verifica se existe um comando else e o imprime.
        if (ctx.getText().contains("senao")) {

            saida.append("else{\n");
            
            // Realiza os comandos do else.
            for (JanderParser.CmdContext cctx : ctx.cmdSenao)
                super.visitCmd(cctx);
            
            saida.append("}\n");
        }

        return null;
    }
    
    @Override
    public Void visitCmdLeia(JanderParser.CmdLeiaContext ctx) {

        TabelaDeSimbolos escopoAtual = tabela;
        String nomeVar;
        TipoJander tipoAuxTipoJander;
        String codigoTipo;
        String str;

        // Executa as verificações dos parâmetros atuais.
        for (JanderParser.IdentificadorContext ictx : ctx.identificador()) {
            nomeVar = ictx.getText();
            tipoAuxTipoJander = escopoAtual.verificar(nomeVar);
            codigoTipo = verificaParamTipoJander(tipoAuxTipoJander);

            // Caso seja um literal, imprime a leitura adequada em C.
            if (tipoAuxTipoJander == TipoJander.LITERAL) {
                str = "gets(" + nomeVar + ");\n";
                saida.append(str);
            // Impressão dos outros tipos básicos.
            } else {
                str = "scanf(\"%" + codigoTipo + "\",&" + nomeVar + ");\n";
                saida.append(str);
            }
        }

        return null;
    }

    @Override
    public Void visitCmdEnquanto(JanderParser.CmdEnquantoContext ctx) {

        saida.append("while(");
        super.visitExpressao(ctx.expressao());
        saida.append("){\n");
        
        // Executa os comandos dentro do while.
        for (JanderParser.CmdContext cctx : ctx.cmd())
            super.visitCmd(cctx);

        saida.append("}\n");

        return null;
    }
    
    @Override
    public Void visitCmdPara(JanderParser.CmdParaContext ctx) {

        String str;
        String nomeVariavel, limiteEsq, limiteDir;

        nomeVariavel = ctx.IDENT().getText();
        limiteEsq = ctx.exp_aritmetica(0).getText();
        limiteDir = ctx.exp_aritmetica(1).getText();

        // Impressão do comando for com os limites obtidos anteriormente.
        str = "for(" + nomeVariavel + " = " + limiteEsq + "; " + nomeVariavel + " <= " + limiteDir + "; " + nomeVariavel + "++){\n";
        saida.append(str);

        // Executa os comandos do for.
        for (JanderParser.CmdContext cctx : ctx.cmd())
            super.visitCmd(cctx);
            
        saida.append("}\n");

        return null;
    }
    
    @Override
    public Void visitCmdFaca(JanderParser.CmdFacaContext ctx) {

        saida.append("do{\n");

        for (JanderParser.CmdContext cctx : ctx.cmd())
            super.visitCmd(cctx);

        saida.append("}while(");
        super.visitExpressao(ctx.expressao());
        saida.append(");\n");

        return null;
    } 
    

    
    @Override
public Void visitCmdEscreva(JanderParser.CmdEscrevaContext ctx) {
    TabelaDeSimbolos escopoAtual = tabela;
    
    for (JanderParser.ExpressaoContext ectx : ctx.expressao()) {
        String str;

        saida.append("printf(\"");

        // Caso seja uma string literal
        if (ectx.getText().contains("\"")) {
            str = ectx.getText().replace("\"", "") + "\");\n";
            saida.append(str);
        } 
        // Caso seja acesso a campo de registro
        else if (ectx.getText().contains(".")) {
            String[] partes = ectx.getText().split("\\.");
            String nomeRegistro = partes[0];
            String nomeCampo = partes[1];
            
            // Verifica se o registro existe na tabela de símbolos
            if (tabela.existe(nomeRegistro)) {
                // Obtém os campos do registro
                Map<String, TipoJander> camposRegistro = tabela.obterCamposDoTipoRegistro(nomeRegistro);

                if (camposRegistro != null && camposRegistro.containsKey(nomeCampo)) {
                    TipoJander tipoCampo = camposRegistro.get(nomeCampo);
                    String codTipoExp = verificaParamTipoJander(tipoCampo);
                    
                    if (codTipoExp != null) {
                        str = "%" + codTipoExp + "\", " + ectx.getText() + ");\n";
                        saida.append(str);
                    } else {
                        // Tipo não reconhecido - tratar erro
                        str = "%d\", " + ectx.getText() + ");\n"; // default para int
                        saida.append(str);
                    }
                } else {
                    // Campo não encontrado - tratar erro
                    str = "%d\", " + ectx.getText() + ");\n"; // default para int
                    saida.append(str);
                }
            } else {
                str = "%d\", " + ectx.getText() + ");\n";
                saida.append(str);
            }
        } 
        // Caso seja uma expressão normal
        else {
            TipoJander tipoAuxTipoJanderExp = verificarTipo(escopoAtual, ectx);
            String codTipoExp = verificaParamTipoJander(tipoAuxTipoJanderExp);
            
            if (codTipoExp != null) {
                if (tipoAuxTipoJanderExp == TipoJander.LITERAL) {
                    str = "%s" + "\", " + ectx.getText() + ");\n";
                } else {
                    str = "%" + codTipoExp + "\", " + ectx.getText() + ");\n";
                }
                saida.append(str);
            } else {
                str = "%d\", " + ectx.getText() + ");\n"; 
                saida.append(str);
            }
        }
    }

    return null;
}
    
    @Override
    public Void visitCmdCaso(JanderParser.CmdCasoContext ctx) {

        String str;
        String limiteEsq, limiteDir;

        str = "switch (" + ctx.exp_aritmetica().getText() + "){\n";
        saida.append(str);

        // Executa os blocos do comando Caso.
        for (JanderParser.Item_selecaoContext sctx : ctx.selecao().item_selecao()) {

            String strOriginal = sctx.constantes().numero_intervalo(0).getText();
            
            // Obtém os limites esquero e direito do caso atual.
            if (strOriginal.contains(".")) {
                limiteEsq = getLimitesCaso(strOriginal, true);
                limiteDir = getLimitesCaso(strOriginal, false);
            // Caso seja um valor único, ambos os limites podem receber o mesmo valor.
            } else {
                limiteEsq = getLimitesCaso(strOriginal, true);
                limiteDir = getLimitesCaso(strOriginal, true);
            }

            if (!sctx.constantes().isEmpty()) {
                for (int i = Integer.parseInt(limiteEsq); i <= Integer.parseInt(limiteDir); i++) {
                    str = "case " + Integer.toString(i) + ":\n";
                    saida.append(str);
                }
            } else {
                str = "case " + limiteEsq + ":\n";
                saida.append(str);
            }
            
            for (JanderParser.CmdContext cctx : sctx.cmd())
                visitCmd(cctx);

            saida.append("break;\n");
        }

        saida.append("default:\n");
        
        for (JanderParser.CmdContext cctx : ctx.cmd())
            visitCmd(cctx);

        saida.append("}\n");

        return null;
    }
    
    @Override
    public Void visitCmdChamada(JanderParser.CmdChamadaContext ctx) {

        String str;
        str = ctx.IDENT().getText() + "(";
        saida.append(str);

        int cont = 0;

        for (JanderParser.ExpressaoContext ectx : ctx.expressao()) {
            if (cont >= 1)
                saida.append(", ");

            saida.append(ectx.getText());
            cont += 1;
        }

        saida.append(");\n");

        return null;
    }
}