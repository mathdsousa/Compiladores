lexer grammar Jander;

// Palavras-chave
ALGORITMO : 'algoritmo';

FIM_ALGORITMO : 'fim_algoritmo';

SE: 'se';

ENTAO: 'entao';

FIM_SE: 'fim_se';

FACA : 'faca';

ATE : 'ate';

PARA : 'para';

FIM_PARA : 'fim_para';

ENQUANTO: 'enquanto';

FIM_ENQUANTO: 'fim_enquanto';

DECLARE : 'declare';

REGISTRO: 'registro';

FIM_REGISTRO: 'fim_registro';

PONTO: '.';

TIPO : 'tipo';

VAR: 'var';

PROCEDIMENTO: 'procedimento';
    
FIM_PROCEDIMENTO: 'fim_procedimento';
    
FUNCAO : 'funcao';

FIM_FUNCSO: 'fim_funcao';

RETORNE : 'retorne';

LITERAL : 'literal';

CONSTANTE : 'constante';

INTEIRO : 'inteiro';

REAL : 'real';

LOGICO : 'logico';

LEIA : 'leia';

ESCREVA : 'escreva';

CASO : 'caso';

SEJA : 'seja';

SENAO : 'senao';

PONTEIRO: '&';

ENDERECO : '^';

FIM_CASO : 'fim_caso';

// Funções matemáticas (reservados na linguagem LA)

COMPRLITERAL : 'comprLiteral';

POSLITERAL : 'posLiteral';

VALLITERAL : 'valLiteral';

// Operadores e símbolos
OP_ARIT : '+' | '-' | '*' | '/' | '%';

OP_REL : '>' | '>=' | '<' | '<=' | '<>' | '=';

OP_LOGICO : 'e' | 'ou' | 'nao';

VERDADEIRO: 'verdadeiro';

FALSO: 'falso';

ATRIB : '<-';

DELIM : ':';

VIRG : ',';

PONTO_PONTO : '..';

ABREPAR : '(';

FECHAPAR : ')';

ABRE_COL : '[';

FECHA_COL : ']';

// Literais e identificadores
CADEIA 	: '"' ( ESC_SEQ | ~('\''|'\\'|'"'|'\n') )* '"';
fragment
ESC_SEQ	: '\\\'';

NUM_INT : [0-9]+;

NUM_REAL : [0-9]+ '.' [0-9]+;

IDENT : [a-zA-Z_][a-zA-Z_0-9]*;

// Comentários e espaços em branco

WS : [ \t\r\n]+ -> skip;

CADEIA_NAO_FECHADA: '"' ( ESC_SEQ | ~('\''|'\\'|'"'| '\n') )* '\n';

COMENTARIO : '{' ( ~'\n' )*? '}' -> skip;

COMENTARIO_NAO_FECHADO: '{' ( ~'}' )*? '\n';

ERRO: .;

