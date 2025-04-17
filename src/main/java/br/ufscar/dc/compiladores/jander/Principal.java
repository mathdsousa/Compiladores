/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package br.ufscar.dc.compiladores.jander;
/**
 *
<<<<<<< HEAD:src/main/java/br/ufscar/dc/compliladores/jander/Principal.java
 * @author Matheus Sousa
 * @author Matheus Cassatti
 * @author Guilherme dos Santos
 */

import br.ufscar.dc.compiladores.jander.Jander;
import java.io.IOException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import javax.crypto.interfaces.PBEKey;

public class Principal {

    public static void main(String[] args) throws IOException {
  
        
        try{
            CharStream cs = CharStreams.fromFileName(args[0]);
            Jander jander = new Jander(cs);
            
            String arquivoSaida = args[1];
            PrintWriter pw = new PrintWriter(arquivoSaida);

            Token t = null;

            while((t = jander.nextToken()).getType() != Token.EOF){
                
                if(Jander.VOCABULARY.getDisplayName(t.getType()).equals("COMENTARIO_NAO_FECHADO"))
                {
                    pw.println("Linha " + t.getLine() + ":" + " comentario nao fechado" );
                    break;
                }
                else if(Jander.VOCABULARY.getDisplayName(t.getType()).equals("CADEIA_NAO_FECHADA"))
                {
                    pw.println("Linha " + t.getLine() + ":" + " cadeia literal nao fechada" );
                    break;
                }
                else if(Jander.VOCABULARY.getDisplayName(t.getType()).equals("ERRO"))
                {
                    pw.println("Linha " + t.getLine() + ": " + t.getText() + " - simbolo nao identificado" );
                    break;
                }
                else{
                    if (Jander.VOCABULARY.getDisplayName(t.getType()) == "OP_ARIT" || Jander.VOCABULARY.getDisplayName(t.getType()) == "OP_REL" || Jander.VOCABULARY.getDisplayName(t.getType()) == "OP_LOGICO") {
                        pw.println("<" + "'" + t.getText() + "'" + "," + "'" + t.getText() + "'" + ">");
                    }
                    else{
                        pw.println("<" + "'" + t.getText() + "'" + "," + Jander.VOCABULARY.getDisplayName(t.getType()) + ">");
                    }
                }
            }
            pw.close();
        }catch(IOException ex){
            
        }
    }
}
