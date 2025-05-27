package br.ufscar.dc.compiladores.jander;

import java.io.PrintWriter;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class MyCustomErrorListener extends BaseErrorListener{
    PrintWriter pw;
    boolean flag = false;

    public MyCustomErrorListener(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void	syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        if(flag)
            return;

        Token t = (Token) offendingSymbol;
        String tempName = JanderLexer.VOCABULARY.getDisplayName(t.getType());

        if(tempName.equals("ERRO")) {
            pw.println("Linha " + t.getLine() + ": " + t.getText() + " - simbolo nao identificado");
        }
        else if(tempName.equals("COMENTARIO_NAO_FECHADO")) {
            pw.println("Linha " + t.getLine() + ": comentario nao fechado");
        }
        else if(tempName.equals("CADEIA_NAO_FECHADA")) {
            pw.println("Linha " + t.getLine() + ": cadeia literal nao fechada");
        }
        else if(t.getText().equals("<EOF>")) {
            pw.println("Linha " + line + ": erro sintatico proximo a EOF");
        }
        else {
            pw.println("Linha " + line + ": erro sintatico proximo a " + t.getText());
        }

        flag = true;
        pw.println("Fim da compilacao");
    }
}