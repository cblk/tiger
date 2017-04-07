package parser;

import jdk.nashorn.internal.objects.NativeUint8Array;
import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

public class Parser {
    private Lexer lexer;
    private Token current;
    private int errors;
    private Token errorToken;

    public Parser(String fileName, java.io.InputStream inputStream) {
        lexer = new Lexer(fileName, inputStream);
        current = lexer.nextToken();
        errors = 0;
    }

    // utility methods to connect the lexer and the parser.
    private void advance() {
        current = lexer.nextToken();
    }

    private void eatToken(Kind kind) {
        if (kind == current.kind)
            advance();
        else {
            error(kind);
        }
    }

    // reports an error to the console
    private void error(Kind kind) {
        // only report error once per erroneous token
        if (current == errorToken)
            return;

        errorToken = current; // set error token to prevent cascading
        errors++; // increment error counter

        // print error report
        System.err.print("ERROR: " + current.kind.toString());
        System.err.print(" at line " + current.getLineNum() + ", column " + current.getColNum());
        if (kind == null) return;
        System.err.println("; Expected " + kind.toString());
        System.exit(0);

    }

/*
    below are method for parsing.
    A bunch of parsing methods to parse expressions. The messy
    parts are to deal with precedence and associativity.
*/

    /**
     * PrimaryExp  ::=  ( Exp )
     * |    Num
     * |    TrueLiteral
     * |    FalseLiteral
     * |    ThisExp
     * |    Identifier
     * |    ArrayAllocationExp
     * |    AllocationExp
     */
    private void parsePrimaryExp() {
        switch (current.kind) {
            case TOKEN_LPAREN:
                advance();
                parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                return;
            case TOKEN_NUM:
                advance();
                return;
            case TOKEN_TRUE:
                advance();
                return;
            case TOKEN_FALSE:
                advance();
                return;
            case TOKEN_THIS:
                advance();
                return;
            case TOKEN_ID:
                advance();
                return;
            case TOKEN_NEW: {
                advance();
                switch (current.kind) {
                    case TOKEN_INT:
                        advance();
                        eatToken(Kind.TOKEN_LBRACK);
                        parseExp();
                        eatToken(Kind.TOKEN_RBRACK);
                        return;
                    case TOKEN_ID:
                        advance();
                        eatToken(Kind.TOKEN_LPAREN);
                        eatToken(Kind.TOKEN_RPAREN);
                        return;
                    default:
                        error(null);
                        return;
                }
            }
            default:
                error(null);
        }
    }

    /**
     * NotExp  ::=  PrimaryExp
     * |    PrimaryExp .id (ExpList)
     * |    PrimaryExp [Exp]
     * |    PrimaryExp .length
     */
    private void parseNotExp() {
        parsePrimaryExp();
        while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
            if (current.kind == Kind.TOKEN_DOT) {
                advance();
                if (current.kind == Kind.TOKEN_LENGTH) {
                    advance();
                    return;
                }
                eatToken(Kind.TOKEN_ID);
                eatToken(Kind.TOKEN_LPAREN);
                parseExpList();
                eatToken(Kind.TOKEN_RPAREN);
            } else {
                advance();
                parseExp();
                eatToken(Kind.TOKEN_RBRACK);
            }
        }
    }

    /**
     * TimesExp  ::=  ! TimesExp
     * |    NotExp
     */
    private void parseTimesExp() {
        while (current.kind == Kind.TOKEN_NOT) {
            advance();
        }
        parseNotExp();
    }

    /**
     * AddSubExp  ::=  AddSubExp * TimesExp
     * |    TimesExp
     */
    private void parseAddSubExp() {
        parseTimesExp();
        while (current.kind == Kind.TOKEN_TIMES) {
            advance();
            parseTimesExp();
        }
    }

    /**
     * LtExp  ::=  LtExp + AddSubExp
     * |    LtExp - AddSubExp
     * |    AddSubExp
     */
    private void parseLtExp() {
        parseAddSubExp();
        while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
            advance();
            parseAddSubExp();
        }
    }

    /**
     * AndExp  ::=  AndExp < LtExp
     * |    LtExp
     */
    private void parseAndExp() {
        parseLtExp();
        while (current.kind == Kind.TOKEN_LT) {
            advance();
            parseLtExp();
        }
    }

    /**
     * Exp  ::=  Exp && AndExp
     * |    AndExp
     */
    private void parseExp() {
        parseAndExp();
        while (current.kind == Kind.TOKEN_AND) {
            advance();
            parseAndExp();
        }
    }

    /**
     * ExpressionList  ::=  Expression ( ExpressionRest )*
     * ExpressionRest  ::=  "," Expression
     */
    private void parseExpList() {
        if (current.kind == Kind.TOKEN_RPAREN)
            return;
        parseExp();
        while (current.kind == Kind.TOKEN_COMMA) {
            advance();
            parseExp();
        }
    }

    /**
     * Statement  ::=  { Statement* }
     * |    if ( Exp ) Statement else Statement
     * |    while ( Exp ) Statement
     * |    System.out.println ( Exp ) ;
     * |    id = Exp ;
     * |    id [ Exp ]= Exp ;
     */
    private void parseStatement() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a statement.
        switch (current.kind) {
            case TOKEN_LBRACE:
                advance();
                parseStatements();
                eatToken(Kind.TOKEN_RBRACE);
                return;
            case TOKEN_IF:
                advance();
                eatToken(Kind.TOKEN_LPAREN);
                parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                parseStatement();
                eatToken(Kind.TOKEN_ELSE);
                parseStatement();
                return;
            case TOKEN_WHILE:
                advance();
                eatToken(Kind.TOKEN_LPAREN);
                parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                parseStatement();
                return;
            case TOKEN_SYSTEM:
                advance();
                eatToken(Kind.TOKEN_DOT);
                eatToken(Kind.TOKEN_OUT);
                eatToken(Kind.TOKEN_DOT);
                eatToken(Kind.TOKEN_PRINTLN);
                eatToken(Kind.TOKEN_LPAREN);
                parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                eatToken(Kind.TOKEN_SEMI);
                return;
            case TOKEN_ID:
                advance();
                if (current.kind == Kind.TOKEN_ASSIGN) {
                    advance();
                    parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                } else if (current.kind == Kind.TOKEN_LBRACK) {
                    advance();
                    parseExp();
                    eatToken(Kind.TOKEN_RBRACK);
                    eatToken(Kind.TOKEN_ASSIGN);
                    parseExp();
                    eatToken(Kind.TOKEN_SEMI);
                } else {
                    error(null);
                }
                return;
            default:
                error(null);
        }
    }

    // Statements -> Statement Statements
    // ->
    private void parseStatements() {
        while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF
                || current.kind == Kind.TOKEN_WHILE
                || current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
            parseStatement();
        }
    }

    /**
     * Type  ::=  int []
     * |    boolean
     * |    int
     * |    id
     */
    private void parseType() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a type.
        switch (current.kind) {
            case TOKEN_INT:
                advance();
                if (current.kind == Kind.TOKEN_LBRACK) {
                    advance();
                    eatToken(Kind.TOKEN_RBRACK);
                }
                return;
            case TOKEN_BOOLEAN:
                advance();
                return;
            case TOKEN_ID:
                advance();
                return;
            default:
                error(null);
        }
    }

    // VarDecl  ::=  Type id ;
    private void parseVarDecl() {
        // to parse the "Type" nonTerminal in this method, instead of writing
        // a fresh one.
        parseType();
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_SEMI);
    }

    // VarDecls  ::=  VarDecl VarDecls
    //           |    VarDecl
    private void parseVarDecls() {
        while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
                || current.kind == Kind.TOKEN_ID) {
            parseVarDecl();
        }
    }

    // FormalList  ::=  Type id FormalRest*
    // FormalRest  ::=  , Type id
    private void parseFormalList() {
        if (current.kind == Kind.TOKEN_RPAREN)
            return;
        parseType();
        eatToken(Kind.TOKEN_ID);
        while (current.kind == Kind.TOKEN_COMMA) {
            advance();
            parseType();
            eatToken(Kind.TOKEN_ID);
        }
    }

    // Method  ::=  public Type id ( FormalList ) { VarDecl* Statement* return Exp ;}
    private void parseMethod() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a method.
        eatToken(Kind.TOKEN_PUBLIC);
        parseType();
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LPAREN);
        parseFormalList();
        eatToken(Kind.TOKEN_RPAREN);
        eatToken(Kind.TOKEN_LBRACE);
        while (current.kind != Kind.TOKEN_RETURN && current.kind != Kind.TOKEN_EOF) {

            switch (current.kind) {

                // int and boolean signals start of var declaration
                case TOKEN_INT:
                case TOKEN_BOOLEAN:
                    parseVarDecl();
                    break;

                // identifier requires peeking at next token to determine if
                // it's a var declaration or a statement in the method
                case TOKEN_ID:
                    // id followed by another id is a var declaration
                    if (lexer.peek().kind == Kind.TOKEN_ID) {
                        parseVarDecl();
                        break;
                    }

                    // otherwise it is a statement, fall through
                default:
                    parseStatement();
            }
        }
        eatToken(Kind.TOKEN_RETURN);
        parseExp();
        eatToken(Kind.TOKEN_SEMI);
        eatToken(Kind.TOKEN_RBRACE);
    }

    // MethodDecls  ::=  MethodDecl MethodDecls
    //              |    MethodDecl
    private void parseMethodDecls() {
        while (current.kind == Kind.TOKEN_PUBLIC) {
            parseMethod();
        }
    }

    /**
     * ClassDecl  ::=  class id { VarDecl* MethodDecl* }
     * |    class id extends id { VarDecl* MethodDecl* }
     */
    private void parseClassDecl() {
        eatToken(Kind.TOKEN_CLASS);
        eatToken(Kind.TOKEN_ID);
        if (current.kind == Kind.TOKEN_EXTENDS) {
            eatToken(Kind.TOKEN_EXTENDS);
            eatToken(Kind.TOKEN_ID);
        }
        eatToken(Kind.TOKEN_LBRACE);
        parseVarDecls();
        parseMethodDecls();
        eatToken(Kind.TOKEN_RBRACE);
    }

    // ClassDecls  ::=  ClassDecl ClassDecls
    //             |    ClassDecl
    private void parseClassDecls() {
        while (current.kind == Kind.TOKEN_CLASS) {
            parseClassDecl();
        }
    }

    // MainClass  ::=  class id { public static void main ( String [] id ) { Statement} }
    private void parseMainClass() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a main class as described by the
        // grammar above.
        eatToken(Kind.TOKEN_CLASS);
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LBRACE);
        eatToken(Kind.TOKEN_PUBLIC);
        eatToken(Kind.TOKEN_STATIC);
        eatToken(Kind.TOKEN_VOID);
        eatToken(Kind.TOKEN_MAIN);
        eatToken(Kind.TOKEN_LPAREN);
        eatToken(Kind.TOKEN_STRING);
        eatToken(Kind.TOKEN_LBRACK);
        eatToken(Kind.TOKEN_RBRACK);
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_RPAREN);
        eatToken(Kind.TOKEN_LBRACE);
        parseStatement();
        eatToken(Kind.TOKEN_RBRACE);
        eatToken(Kind.TOKEN_RBRACE);
    }

    // Program  ::=  MainClass ClassDecl*
    private void parseProgram() {
        parseMainClass();
        parseClassDecls();
        eatToken(Kind.TOKEN_EOF);
    }

    public void parse() {
        parseProgram();
        if (errors == 0) {
            System.out.println("No error!");
        }
    }
}
