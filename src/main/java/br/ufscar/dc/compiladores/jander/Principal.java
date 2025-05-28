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
import java.io.PrintWriter;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;

public class Principal {

    public static void main(String[] args) throws IOException {
        
        // Verifica se foram passados os argumentos necessários
        if (args.length != 2) {
            System.err.println("Erro: É necessário fornecer dois argumentos: o arquivo de entrada e o arquivo de saída.");
            return;
        }
        
        try {
            // Cria o fluxo de caracteres a partir do arquivo de entrada
            CharStream cs = CharStreams.fromFileName(args[0]);
            JanderLexer lexer = new JanderLexer(cs);

            // Obtém o nome do arquivo de saída
            String arquivoSaida = args[1];
            try (PrintWriter pw = new PrintWriter(arquivoSaida)) {
                // Cria o token stream e o parser
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                JanderParser parser = new JanderParser(tokens);
                
                // Adiciona o listener de erros personalizado
                MyCustomErrorListener mcel = new MyCustomErrorListener(pw);
                parser.removeErrorListeners(); // Remove os listeners de erro padrão
                parser.addErrorListener(mcel); // Adiciona o listener customizado
                
                // Inicia a análise sintática do programa
                JanderParser.ProgramaContext ctx = parser.programa();
                
                // Cria o visitante semântico e passa a tabela de símbolos
                JanderSemantico jander = new JanderSemantico();
                jander.visitPrograma(ctx);
                
                // Imprime os erros semânticos
                if (!JanderSemanticoUtils.errosSemanticos.isEmpty()) {
                    JanderSemanticoUtils.errosSemanticos.forEach(pw::println);
                } else {
                    pw.println("Nenhum erro semantico encontrado.");
                }

                // Finaliza a compilação
                pw.println("Fim da compilacao");
            }
        } catch (IOException ex) {
            System.err.println("Erro ao processar o arquivo: " + ex.getMessage());
            ex.printStackTrace();
        } catch (RecognitionException ex) {
            System.err.println("Erro inesperado: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}