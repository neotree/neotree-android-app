grammar BooleanExpression;

root
    : expression* EOF
    ;

expression
    : LPAREN expression RPAREN
    | NOT expression
    | left=expression and=binaryAnd right=expression
    | left=expression or=binaryOr right=expression
    | assertion
    ;

assertion
    : id=identifier op=operation valueBool=booleanLiteral
    | id=identifier op=operation valueString=stringLiteral
    | id=identifier op=operation valueNumber=numberLiteral
    | id=identifier op=operation valueIdentifier=identifier
    ;

identifier
    : IdentifierLiteral
    ;

binaryAnd
    : AND
    ;

binaryOr
    : OR
    ;

operation
    : EQ
    | NEQ
    | GT
    | GTE
    | LT
    | LTE
    ;

numberLiteral
    : NumberLiteral
    ;

stringLiteral
    : StringLiteral
    ;

booleanLiteral
    : BooleanLiteral
    ;

/* Lexer Rules */
IdentifierLiteral
    : '$'(LETTER| '_') (LETTER|DIGIT|'_'|'.')*
    ;

NumberLiteral
    : IntConst
    | DoubleConst
    ;

StringLiteral
    : SingleQuoteStringConst
//    | DoubleQuoteStringConst
    | NilConst
    ;

BooleanLiteral
    : 'false'
    | 'FALSE'
    | 'no'
    | 'NO'
    | 'true'
    | 'TRUE'
    | 'yes'
    | 'YES'
    ;

NilConst
    : 'nil'
    | 'null'
    ;

IntConst
    : ('-')? NumberNotLeadingZero
    ;

DoubleConst
    : NumberNotLeadingZero (DOT DIGIT+)?
    ;

SingleQuoteStringConst
    : '\'' (~'\'')*? '\''
    ;

//DoubleQuoteStringConst
//    : '"' (~'"')* '"'
//    ;

NumberNotLeadingZero
    : DIGIT | (DIGIT_NOT_ZERO DIGIT+)
    ;

// Symbols
NOT     : ('not' | 'NOT');
AND     : ('and' | 'AND');
OR      : ('or' | 'OR');
LPAREN  : '(';
RPAREN  : ')';
GT      : '>';
GTE     : '>=';
LT      : '<';
LTE     : '<=';
EQ      : '=';
NEQ     : '!=';
DOT     : '.';
WS      : [ \r\t\u000C\n]+ -> skip ;

fragment LETTER
    : ('a'..'z' | 'A'..'Z')
    ;

fragment DIGIT
    : ('0'..'9')
    ;

fragment DIGIT_NOT_ZERO
    : ('1'..'9')
    ;

