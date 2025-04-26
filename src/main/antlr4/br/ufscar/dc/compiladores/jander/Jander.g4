grammar Jander;

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

FIM_FUNCAO: 'fim_funcao';

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

ENDERECO: '&';

PONTEIRO : '^';

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


//classificação dos erros para identificação dos mesmos
CADEIA_NAO_FECHADA: '"' ( ESC_SEQ | ~('\''|'\\'|'"'| '\n') )* '\n';

COMENTARIO : '{' ( ~'\n' )*? '}' -> skip; // classificação para o comentario valer apenas na mesma linha

COMENTARIO_NAO_FECHADO: '{' ( ~'}' )*? '\n';

ERRO: .;

// Regras 
programa
    : declaracoes ALGORITMO corpo FIM_ALGORITMO EOF
    ;

declaracoes
    : (declaracao_global | declaracao_local)*
    ;

declaracao_global
    : PROCEDIMENTO IDENT ABREPAR parametros? FECHAPAR declaracao_local* comando* FIM_PROCEDIMENTO
    | FUNCAO IDENT ABREPAR parametros? FECHAPAR DELIM tipo_ponteiro declaracao_local* comando* FIM_FUNCAO
    ;

declaracao_local
    : DECLARE variavel
    | CONSTANTE IDENT DELIM tipo_basico '=' valor_constante
    | TIPO IDENT DELIM tipo
    ;

variavel
    : identificador (VIRG identificador)* DELIM tipo
    ;

identificador
    : IDENT (PONTO IDENT)* tamanho
    ;

tamanho
    : (ABRE_COL expressao_aritmetica FECHA_COL)*
    ;

valor_constante
    : CADEIA | NUM_INT | NUM_REAL | VERDADEIRO | FALSO
    ;

tipo
    : tipo_registro | tipo_ponteiro
;

tipo_basico
    : LITERAL
    | INTEIRO
    | REAL
    | LOGICO
    ;

tipo_ponteiro
    : PONTEIRO? (tipo_basico | IDENT)?
    ;

tipo_registro
    : REGISTRO variavel* FIM_REGISTRO
    ;

parametro
    : VAR? identificador (VIRG identificador)* DELIM tipo_ponteiro;

parametros
    : parametro (VIRG parametro)*
    ;

corpo
    : declaracao_local* comando*
    ;

comando_leia
    : LEIA ABREPAR PONTEIRO? identificador (VIRG PONTEIRO? identificador)* FECHAPAR
    ;

comando_escreva
    : ESCREVA ABREPAR expressao (VIRG expressao)* FECHAPAR
    ;

comando_se
    : SE expressao ENTAO comando* (SENAO comando*)? FIM_SE
    ;

comando_caso
    : CASO expressao_aritmetica SEJA selecao (SENAO comando*)? FIM_CASO
    ;

comando_para
    : PARA IDENT ATRIB expressao_aritmetica ATE expressao_aritmetica FACA comando* FIM_PARA
    ;

comando_enquanto
    : ENQUANTO expressao FACA comando* FIM_ENQUANTO
    ;

comando_faca
    : FACA comando ATE expressao
    ;

comando_atribuicao
    : PONTEIRO? identificador ATRIB expressao
    ;

comando_chamada
    : IDENT ABREPAR expressao (VIRG expressao)* FECHAPAR
    ; 

comando_retorno
    : RETORNE expressao
    ;

comando
    : comando_leia
    | comando_escreva
    | comando_se
    | comando_caso
    | comando_para
    | comando_enquanto
    | comando_faca
    | comando_atribuicao
    | comando_chamada 
    | comando_retorno
    ;

selecao
    : item_selecao*
    ;

item_selecao
    : constantes DELIM comando*
    ;

constantes
    : numero_intervalo (VIRG numero_intervalo)*
    ;

numero_intervalo
    : '-'? NUM_INT (PONTO_PONTO '-'? NUM_INT)?
    ;

expressao_aritmetica
    : termo (operador_1 termo)*
    ;

termo
    : fator (operador_2 fator)*
;

fator
    : parcela (operador_3 parcela)*
    ;

operador_1
    : '+' | '-';

operador_2
    : '*' | '/';

operador_3
    : '%';

parcela
    : '-'? parcela_unaria | parcela_nao_unaria
    ;
 
parcela_unaria
    : '^'? identificador
    | IDENT ABREPAR expressao (VIRG expressao)* FECHAPAR
    | NUM_INT
    | NUM_REAL
    | ABREPAR expressao FECHAPAR
    ;

parcela_nao_unaria
    :'&' identificador | CADEIA;


expressao
    : termo_logico (operador_logico_1 termo_logico)*
    ;

expressao_relacional
    : expressao_aritmetica (OP_REL expressao_aritmetica)?
    ;

termo_logico
    : fator_logico (operador_logico_2 fator_logico)*
    ;

fator_logico 
    : 'nao'? parcela_logica
    ;

parcela_logica 
    : ('verdadeiro' | 'falso') | expressao_relacional
    ;

operador_logico_1 
    : 'ou'
    ;

operador_logico_2 
    : 'e'
;  



