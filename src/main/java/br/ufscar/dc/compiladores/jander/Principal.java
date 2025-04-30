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
        
        try {
            CharStream cs = CharStreams.fromFileName(args[0]);
            JanderLexer lexer = new JanderLexer(cs);

            String arquivoSaida = args[1];
            PrintWriter pw = new PrintWriter(arquivoSaida);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JanderParser parser = new JanderParser(tokens);

            // Adiciona o listener customizado
            MyCustomErrorListener mcel = new MyCustomErrorListener(pw);
            parser.removeErrorListeners(); // Remove o padrão
            parser.addErrorListener(mcel);

            parser.programa(); // inicia a análise

            pw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
