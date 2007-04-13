grammar SweetXml;

@parser::header { package net.innig.sweetxml.parser; }
@lexer::header  { package net.innig.sweetxml.parser; }

sweetXml
    : directives
      line (EOL line)*
    ;

directives
    : (nestedAngleBraces | S | EOL)*
    ;

protected
nestedAngleBraces
    : '<' (nestedAngleBraces | ~('<'|'>') )* '>'
    ;
    
line
    : indent
      element?
      S*
      comment?
    ;

indent
    : S*
    ;

element
    : tag
    | quotedText;
    
tag
    : NAME attributes? S* inlineText?
    ;

protected
attributes
    : S+ attribute attributes?
    ;

attribute
    :  NAME S* '=' S* text
    ;
    
inlineText
    : ':' S* (text)?
    ;

text
    : quotedText
    | unquotedText
    ;

quotedText
    : '"' (~('"'))* '"'
    | '\'' (~('\''))* '\''
    ;
    
unquotedText
    : (NAME_START | NAME_CONT)+
    ;

comment
    : '#' (~(EOL))*;
    
NAME
    : NAME_START (NAME_START | NAME_CONT)*
    ;

fragment
NAME_START
    : 'A'..'Z' | 'a'..'z' | '_'
    | '\u00C0'..'\u00D6' | '\u00D8'..'\u00F6' | '\u00F8'..'\u02FF' | '\u0370'..'\u037D'
    | '\u037F'..'\u1FFF' | '\u200C'..'\u200D' | '\u2070'..'\u218F' | '\u2C00'..'\u2FEF'
    | '\u3001'..'\uD7FF' | '\uF900'..'\uFDCF' | '\uFDF0'..'\uFFFD'
    ;

fragment
NAME_CONT
    : '0'..'9' | '-' | '.'
    | '\u00B7' | '\u0300'..'\u036F' | '\u203F'..'\u2040'
    ;

S  : ' '
    | '\t';

EOL
    : '\r\n'
    | '\r'
    | '\n'
    ;
