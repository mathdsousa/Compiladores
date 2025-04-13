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

public class Principal {

    public static void main(String[] args) throws IOException {
        CharStream cs = CharStreams.fromFileName(args[0]);
        Jander jander = new Jander(cs);
        while(jander.nextToken().getType() != Token.EOF){
            System.out.print("");
        }
            
    }
}
