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

import java.io.IOException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import javax.crypto.interfaces.PBEKey;
import javax.sql.CommonDataSource;
import javax.swing.border.EmptyBorder;
import org.antlr.v4.runtime.CommonTokenStream;

public class Principal {

    public static void main(String[] args) throws IOException {
        
        try{
           //criação do arquivo de texto e organização para compilação
            CharStream cs = CharStreams.fromFileName(args[0]);
            JanderLexer lexer = new JanderLexer(cs);
          
            String arquivoSaida = args[1];
            PrintWriter pw = new PrintWriter(arquivoSaida);

            Token t = null;
            
            //leitura dos arquivo de texto ate o TOKEN.EOF
            while((t = lexer.nextToken()).getType() != Token.EOF){
                
                // Verificação de erros antes de especificar o token
                if(JanderLexer.VOCABULARY.getDisplayName(t.getType()).equals("COMENTARIO_NAO_FECHADO"))
                {
                    pw.println("Linha " + t.getLine() + ":" + " comentario nao fechado" );
                    break;
                }
                else if(JanderLexer.VOCABULARY.getDisplayName(t.getType()).equals("CADEIA_NAO_FECHADA"))
                {
                    pw.println("Linha " + t.getLine() + ":" + " cadeia literal nao fechada" );
                    break;
                }
                else if(JanderLexer.VOCABULARY.getDisplayName(t.getType()).equals("ERRO"))
                {
                    pw.println("Linha " + t.getLine() + ": " + t.getText() + " - simbolo nao identificado" );
                    break;
                }
                else{ // após a verificação de erro, é classificado o TOKEN
                    if (JanderLexer.VOCABULARY.getDisplayName(t.getType()) == "OP_ARIT" || JanderLexer.VOCABULARY.getDisplayName(t.getType()) == "OP_REL" || JanderLexer.VOCABULARY.getDisplayName(t.getType()) == "OP_LOGICO") {
                        pw.println("<" + "'" + t.getText() + "'" + "," + "'" + t.getText() + "'" + ">"); // mudança para a saída do T1 no qual os operadores  são considerados palavra-chave na saída
                    }
                    else{
                        pw.println("<" + "'" + t.getText() + "'" + "," + JanderLexer.VOCABULARY.getDisplayName(t.getType()) + ">"); // classificação dos TOKENS
                    }
                }
            }
            
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JanderParser parser = new JanderParser(tokens);
            parser.programa();
            
            pw.close();
            
        }catch(IOException ex){
            
        }
    }
}
