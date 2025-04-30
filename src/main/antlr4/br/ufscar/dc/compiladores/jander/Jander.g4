grammar Jander;

// Palavras-chave
ALGORITMO : 'algoritmo';
DECLARE : 'declare';
LITERAL : 'literal';
INTEIRO : 'inteiro';
REAL : 'real';
LOGICO : 'logico';
LEIA : 'leia';
ESCREVA : 'escreva';
SE : 'se';
FIM_SE : 'fim_se';
ENTAO: 'entao';
CASO : 'caso';
SEJA : 'seja';
SENAO : 'senao';
FIM_CASO : 'fim_caso';
PARA : 'para';
ATE : 'ate';
FACA : 'faca';
FIM_PARA : 'fim_para';
ENQUANTO : 'enquanto';
FIM_ENQUANTO : 'fim_enquanto';
REGISTRO : 'registro';
FIM_REGISTRO : 'fim_registro';
TIPO : 'tipo';
PROCEDIMENTO : 'procedimento';
FIM_PROCEDIMENTO : 'fim_procedimento';
VAR : 'var';
FUNCAO : 'funcao';
RETORNE : 'retorne';
FIM_FUNCAO : 'fim_funcao';
CONSTANTE : 'constante';
VERDADEIRO : 'verdadeiro';
FALSO : 'falso';
FIM_ALGORITMO : 'fim_algoritmo';

// Operadores e símbolos
OP_ARIT : '+' | '-' | '*' | '/' | '%';
OP_REL : '>' | '>=' | '<' | '<=' | '<>' | '=';
OP_LOGICO : 'e' | 'ou' | 'nao';
ATRIB : '<-';
DELIM : ':';
VIRG : ',';
PONTO_PONTO : '..';
PONTO: '.';
ABREPAR : '(';
FECHAPAR : ')';
ABRE_COL : '[';
FECHA_COL : ']';
ENDERECO: '&';
PONTEIRO : '^';

// Literais e identificadores
CADEIA : '"' ( ESC_SEQ | ~('\n'|'\''|'\\'|'"') )* '"';
fragment ESC_SEQ : '\\\'';
NUM_INT : [0-9]+;
NUM_REAL : [0-9]+ ('.' [0-9]+)?;
IDENT : [a-zA-Z_][a-zA-Z_0-9]*;

// Comentários e espaços em branco
COMENTARIO : '{' ~('}'|'\n')* '}' -> skip;
WS : [ \t\r\n]+ -> skip;

// Classificação de erros
CADEIA_NAO_FECHADA: '"' ( ESC_SEQ | ~('\n'|'\''|'\\'|'"') )* '\n';
COMENTARIO_NAO_FECHADO: '{' ~('}'|'\n')* '\n';
ERRO: .;

// Regras
programa : declaracoes ALGORITMO corpo FIM_ALGORITMO EOF;
declaracoes : decl_local_global*;
decl_local_global : declaracao_local | declaracao_global;
declaracao_local : DECLARE variavel
                 | CONSTANTE IDENT DELIM tipo_basico '=' valor_constante
                 | TIPO IDENT DELIM tipo;
variavel : identificador (VIRG identificador)* DELIM tipo;
identificador : IDENT (PONTO IDENT)* dimensao;
dimensao : (ABRE_COL exp_aritmetica FECHA_COL)*;
tipo : registro | tipo_estendido;
tipo_basico : LITERAL | INTEIRO | REAL | LOGICO;
tipo_basico_ident : tipo_basico | IDENT;
tipo_estendido : PONTEIRO? tipo_basico_ident;
valor_constante : CADEIA | NUM_INT | NUM_REAL | VERDADEIRO | FALSO;
registro : REGISTRO variavel* FIM_REGISTRO;
declaracao_global : PROCEDIMENTO IDENT ABREPAR parametros? FECHAPAR declaracao_local* cmd* FIM_PROCEDIMENTO
                  | FUNCAO IDENT ABREPAR parametros? FECHAPAR DELIM tipo_estendido declaracao_local* cmd* FIM_FUNCAO;
parametro : VAR? identificador (VIRG identificador)* DELIM tipo_estendido;
parametros : parametro (VIRG parametro)*;
corpo : declaracao_local* cmd*;
cmd : cmdLeia | cmdEscreva | cmdSe | cmdCaso | cmdPara | cmdEnquanto | cmdFaca | cmdAtribuicao | cmdChamada | cmdRetorne;
cmdLeia : LEIA ABREPAR PONTEIRO? identificador (VIRG PONTEIRO? identificador)* FECHAPAR;
cmdEscreva : ESCREVA ABREPAR expressao (VIRG expressao)* FECHAPAR;
cmdSe : SE expressao ENTAO cmd* (SENAO cmd*)? FIM_SE;
cmdCaso : CASO exp_aritmetica SEJA selecao (SENAO cmd*)? FIM_CASO;
cmdPara : PARA IDENT ATRIB exp_aritmetica ATE exp_aritmetica FACA cmd* FIM_PARA;
cmdEnquanto : ENQUANTO expressao FACA cmd* FIM_ENQUANTO;
cmdFaca : FACA cmd* ATE expressao;
cmdAtribuicao : PONTEIRO? identificador ATRIB expressao;
cmdChamada : IDENT ABREPAR expressao (VIRG expressao)* FECHAPAR;
cmdRetorne : RETORNE expressao;

selecao: item_selecao*;
item_selecao : constantes DELIM cmd*;
constantes : numero_intervalo (VIRG numero_intervalo)*;
numero_intervalo : op_unario? NUM_INT (PONTO_PONTO op_unario? NUM_INT)?;
op_unario : '-';

exp_aritmetica : termo (op1 termo)*;
termo : fator (op2 fator)*;
fator : parcela (op3 parcela)*;
op1 : '+' | '-';
op2 : '*' | '/';
op3 : '%';

parcela : op_unario? parcela_unario | parcela_nao_unario;
parcela_unario : PONTEIRO? identificador
               | IDENT ABREPAR expressao (VIRG expressao)* FECHAPAR
               | NUM_INT
               | NUM_REAL
               | ABREPAR expressao FECHAPAR;
parcela_nao_unario : ENDERECO identificador | CADEIA;

exp_relacional : exp_aritmetica (op_relacional exp_aritmetica)?;
op_relacional : '=' | '<>' | '>=' | '<=' | '>' | '<';

expressao : termo_logico (op_logico_1 termo_logico)*;
termo_logico : fator_logico (op_logico_2 fator_logico)*;
fator_logico : 'nao'? parcela_logica;
parcela_logica : VERDADEIRO | FALSO | exp_relacional;
op_logico_1 : 'ou';
op_logico_2 : 'e';