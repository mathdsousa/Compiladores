lexer grammar Jander;

// Palavras-chave
ALGORITMO : 'algoritmo';

FIM_ALGORITMO : 'fim_algoritmo';

DECLARE : 'declare';

LITERAL : 'literal';

INTEIRO : 'inteiro';

REAL : 'real';

LOGICO : 'logico';

LEIA : 'leia';

ESCREVA : 'escreva';

CASO : 'caso';

SEJA : 'seja';

SENAO : 'senao';

FIM_CASO : 'fim_caso';

// Funções matemáticas (reservados na linguagem LA)
RAIZ : 'raiz';

SEN : 'sen';

COS : 'cos';

TAN : 'tan';

ASEN : 'asen';

ACOS : 'acos';

ATAN : 'atan';

LN : 'ln';

LOG : 'log';

EXP : 'exp';

POT : 'pot';

SINAL : 'sinal';

TRUNCA : 'trunca';

INT : 'int';

FRAC : 'frac';

ARRED : 'arred';

ABS : 'abs';

RESTO : 'resto';

QUOC : 'quoc';

COMPRLITERAL : 'comprLiteral';

POSLITERAL : 'posLiteral';

VALLITERAL : 'valLiteral';


// Literais e identificadores
CADEIA     : '"' ( ESC_SEQ | ~['"'] )* '"' ;
fragment ESC_SEQ : '\\' [bfnrt"'\\] ;

NUM_INT : [0-9]+;

NUM_REAL : [0-9]+ '.' [0-9]+;

IDENT : [a-zA-Z_][a-zA-Z_0-9]*;

// Comentários e espaços em branco
COMENTARIO : '{' .*? '}' -> skip;

WS : [ \t\r\n]+ -> skip;

// Operadores e símbolos
OP_ARIT : '+' | '-' | '*' | '/';

OP_REL : '>' | '>=' | '<' | '<=' | '<>' | '=';

OP_LOGICO : 'e' | 'ou' | 'nao';

ATRIB : '<-';

DELIM : ':';

VIRG : ',';

PONTO_PONTO : '..';

ABREPAR : '(';

FECHAPAR : ')';