package lexer;

import static control.Control.ConLexer.dump;

import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import lexer.Token.Kind;
import util.Todo;

public class Lexer {
    private String fname; // the input file name to be compiled
    private InputStream fstream; // input stream for the above file
    private int lineNum;
    private int nextChar;
    // hash tables for fast lookup
    private final static Map<String, Kind> reservedWords;
    private final static Map<Character, Kind> punctuation;

    // initialize hash tables statically
    static {
        reservedWords = new HashMap<>();
        reservedWords.put("boolean", Kind.TOKEN_BOOLEAN);
        reservedWords.put("class", Kind.TOKEN_CLASS);
        reservedWords.put("else", Kind.TOKEN_ELSE);
        reservedWords.put("extends", Kind.TOKEN_EXTENDS);
        reservedWords.put("false", Kind.TOKEN_FALSE);
        reservedWords.put("if", Kind.TOKEN_IF);
        reservedWords.put("int", Kind.TOKEN_INT);
        reservedWords.put("main", Kind.TOKEN_MAIN);
        reservedWords.put("new", Kind.TOKEN_NEW);
        reservedWords.put("out", Kind.TOKEN_OUT);
        reservedWords.put("public", Kind.TOKEN_PUBLIC);
        reservedWords.put("println", Kind.TOKEN_PRINTLN);
        reservedWords.put("return", Kind.TOKEN_RETURN);
        reservedWords.put("static", Kind.TOKEN_STATIC);
        reservedWords.put("String", Kind.TOKEN_STRING);
        reservedWords.put("System", Kind.TOKEN_SYSTEM);
        reservedWords.put("this", Kind.TOKEN_THIS);
        reservedWords.put("true", Kind.TOKEN_TRUE);
        reservedWords.put("void", Kind.TOKEN_VOID);
        reservedWords.put("while", Kind.TOKEN_WHILE);

        punctuation = new HashMap<>();
        punctuation.put('(', Kind.TOKEN_LPAREN);
        punctuation.put(')', Kind.TOKEN_RPAREN);
        punctuation.put('[', Kind.TOKEN_LBRACK);
        punctuation.put(']', Kind.TOKEN_RBRACK);
        punctuation.put('{', Kind.TOKEN_LBRACE);
        punctuation.put('}', Kind.TOKEN_RBRACE);
        punctuation.put(';', Kind.TOKEN_SEMI);
        punctuation.put(',', Kind.TOKEN_COMMER);
        punctuation.put('.', Kind.TOKEN_DOT);
        punctuation.put('=', Kind.TOKEN_ASSIGN);
        punctuation.put('!', Kind.TOKEN_NOT);
    }

    public Lexer(String fname, InputStream fstream) {
        this.fname = fname;
        this.fstream = fstream;
        lineNum = 1;
        nextChar = getChar();
    }

    private int getChar() {
        try {
            return fstream.read();
        } catch (IOException e) {
            System.err.println("IOException occured in Lexer::getChar()");
            return -1;
        }
    }

    // detect and skip possible '\n', '\r' and '\rn' line breaks
    private void skipNewline() {
        if (nextChar == '\n') {
            lineNum++;
            nextChar = getChar();
            return;
        }

        if (nextChar == '\r') {
            lineNum++;
            nextChar = getChar();
            // skip over next char if '\n'
            if (nextChar == '\n')
                nextChar = getChar();
            return;
        }
        nextChar = getChar();
    }

    // When called, return the next token (refer to the code "Token.java")
    // from the input stream.
    // Return TOKEN_EOF when reaching the end of the input stream.
    private Token nextTokenInternal() throws Exception {
        if (-1 == nextChar)
            return new Token(Kind.TOKEN_EOF, lineNum);

        // skip all kinds of "blanks"
        while (Character.isWhitespace(nextChar)) {
            skipNewline();
        }
        if (-1 == nextChar)
            return new Token(Kind.TOKEN_EOF, lineNum);

        // identifier or reserved word ([a-zA-Z][a-zA-Z0-9_]*)
        if (Character.isLetter(nextChar)) {
            // create new idVal starting with first char of identifier
            String idVal = Character.toString((char) nextChar);
            nextChar = getChar();

            // include remaining seq. of chars that are letters, digits, or _
            while (Character.isLetterOrDigit(nextChar) || nextChar == '_') {
                idVal += (char) nextChar;
                nextChar = getChar();
            }

            // check if identifier is a reserved word
            Kind type = reservedWords.get(idVal);
            if (type != null)
                return new Token(type, lineNum);
            else // token is an identifier
                return new Token(Kind.TOKEN_ID, lineNum, idVal);
        }

        // integer literal ([0-9]+)
        if (Character.isDigit(nextChar)) {
            // create string representation of number
            String numString = Character.toString((char) nextChar);
            nextChar = getChar();

            // concatenate remaining seq. of digits
            while (Character.isDigit(nextChar)) {
                numString += (char) nextChar;
                nextChar = getChar();
            }
            return new Token(Kind.TOKEN_INT, lineNum, numString);
        }

        // check for binOps
        switch (nextChar) {
            case '+':
                nextChar = getChar();
                return new Token(Kind.TOKEN_ADD, lineNum);
            case '-':
                nextChar = getChar();
                return new Token(Kind.TOKEN_SUB, lineNum);
            case '*':
                nextChar = getChar();
                return new Token(Kind.TOKEN_TIMES, lineNum);
            case '<':
                nextChar = getChar();
                return new Token(Kind.TOKEN_LT, lineNum);
            case '&':
                nextChar = getChar();
                // check if next char is '&' to match '&&' binop
                if (nextChar == '&') {
                    nextChar = getChar();
                    return new Token(Kind.TOKEN_AND, lineNum);
                } else
                    return new Token(Kind.TOKEN_UNKNOWN, lineNum);
        }

        // check for punctuation
        Kind type = punctuation.get((char) nextChar);
        nextChar = getChar();

        // found punctuation token
        if (type != null)
            return new Token(type, lineNum);

        // token type is unknown
        return new Token(Kind.TOKEN_UNKNOWN, lineNum);
    }

    public Token nextToken() {
        Token t = null;

        try {
            t = this.nextTokenInternal();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (dump)
            System.out.println(t.toString());
        return t;
    }


}
