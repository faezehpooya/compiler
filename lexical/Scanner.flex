package lexical;
import java.util.Arrays;
import java.io.IOException;
import java.util.List;
import syntax.Lexical;


%%
%class LexicalAnalyzer
%unicode
%line
%column
%type Symbol
%function next_token
%implements Lexical
%public

%{
    private StringBuffer string = new StringBuffer();
    private Symbol currentSymbol = null;
    private List<String> words;

    private Symbol symbol(String token) {
        return new Symbol(token);
    }

    private Symbol symbol(String token, Object val) {
        return new Symbol(token, val);
    }

    public Symbol currentToken() {
        return currentSymbol;
    }

    public String nextToken() {
        try {
            currentSymbol = next_token();
            return currentSymbol.getToken();
        } catch (IOException e) {
            throw new RuntimeException("Unable to get next token", e);
        }
    }

%}

%init{
	words = Arrays.asList("array", "assign", "boolean", "break", "begin", "char", "continue",
			"do", "else", "end", "function", "procedure", "if", "real", "of", "integer", "return",
                        "string", "long", "while", "var", "then");
%init}

%eofval{
     return symbol("$");
%eofval}

%{
public String lexeme;
%}

LineTerminator = \r|\n|\r\n
WhiteSpace = {LineTerminator} |  [ \t\r\n]

Zero = 0
DecInt = [1-9][0-9]*
HexInt = 0[xX][0-9a-fA-F]+
Integer = ( {Zero} | {DecInt} | {HexInt} )[lL]?
Exponent = [eE] [\+\-]? [0-9]+
Float1 = [0-9]+ \. [0-9]+ {Exponent}?
Float2 = \. [0-9]+ {Exponent}?
Float3 = [0-9]+ \. {Exponent}?
Float4 = [0-9]+ {Exponent}
Float = ( {Float1} | {Float2} | {Float3} | {Float4} ) [fFdD]? | [0-9]+ [fFDd]
Id = [A-Za-z_] [A-Za-z_0-9]*

CChar = [^\'\\\n\r] | {EscChar}
SChar = [^\"\\\n\r] | {EscChar}
EscChar = \\[ntbrf\\\'\"] | {OctalEscape}
OctalEscape = \\[0-7] | \\[0-7][0-7] | \\[0-3][0-7][0-7]

 /* comments */

line_comment_one =      "//"~\n
line_comment_two =          --~\n
begin_comment =         <--
end_comment =           -->
Comment =               ( {begin_comment}~{end_comment} | {line_comment_one} | {line_comment_two})
%%

<YYINITIAL> {
    {Comment}			{ /* skip */ }
	"("                             { return symbol("lparan"); }
	")"                             { return symbol("rparan"); }
	"[" 				{ return symbol("lbrack", "["); }
	"]" 				{ return symbol("rbrack"); }
	"+"                             { return symbol("plus"); }
	"-"                             { return symbol("minus"); }
	"*"                             { return symbol("multiply"); }
	"/"                             { return symbol("divide"); }
	"&" 				{ return symbol("band"); }
	"^" 				{ return symbol("eadd"); }
	"|" 				{ return symbol("bor"); }
	"and" 				{ return symbol("and"); }
	"or" 				{ return symbol("or"); }
	"%"                             { return symbol("mod"); }
	"~" 				{ return symbol("lnot"); }

	"<" 				{ return symbol("cmp_l"); }
	">=" 				{ return symbol("cmp_geq"); }
	"=" 				{ return symbol("cmp_eq"); }
	"<>" 				{ return symbol("cmp_neq"); }
	"<=" 				{ return symbol("cmp_leq"); }
	">" 				{ return symbol("cmp_g"); }
	"=="				{ return symbol("eqeq"); }

	":" 				{ return symbol("typifier"); }
	":=" 				{ return symbol("assign"); }
	"," 				{ return symbol("comma"); }
	";" 				{ return symbol("semicolon"); }

	
	"true" 				{ return symbol("bool", true); }
	"false" 			{ return symbol("bool", false); }

	{Integer} 			{ // TODO to know what is length of long
						if(yylength() < 10){
							return symbol("int", Integer.parseInt(yytext()));
						}
						return symbol("lng", Integer.parseInt(yytext()));
					}
	{Float} 			{ return symbol("float", Float.parseFloat(yytext())); }
	{Id} 				{
						if(words.contains(yytext())){
							return symbol(yytext());
						} 
						return symbol("id", String.valueOf(yytext()));
					}

	\'{CChar}\' 			{
						String lexeme = yytext();
						return symbol("chr", lexeme.charAt(1)); 
					}

	\"{SChar}*\" 			{
						String lexeme = yytext();
						return symbol("str", lexeme.substring(1,lexeme.length()-1)); 
					}

 
	/* whitespace */
	{WhiteSpace}+                   {/* skip */ }

}

/* error fallback */
[^]                                 { throw new Error("Illegal character <" + yytext() + ">"); }

