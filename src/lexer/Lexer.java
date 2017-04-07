package lexer;

import static control.Control.ConLexer.dump;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import lexer.Token.Kind;

public class Lexer {
    private String fileName; // the input file name to be compiled
    private InputStream fStream; // input stream for the above file
    private int lineNum;
    private int colNum;
    private int nextChar;
    private Token next;
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
        reservedWords.put("length", Kind.TOKEN_LENGTH);
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
        punctuation.put(',', Kind.TOKEN_COMMA);
        punctuation.put('.', Kind.TOKEN_DOT);
        punctuation.put('=', Kind.TOKEN_ASSIGN);
        punctuation.put('!', Kind.TOKEN_NOT);
    }

    public Lexer(String fName, InputStream fStream) {
        this.fileName = fName;
        this.fStream = fStream;
        lineNum = 1;
        colNum = 0;
        nextChar = getChar();
    }

    private int getChar() {
        try {
            int c = fStream.read();
            colNum++;
            return c;
        } catch (IOException e) {
            System.err.println("IOException occured in Lexer::getChar()");
            return -1;
        }
    }

    // detect and skip possible '\n', '\r' and '\rn' line breaks
    private boolean skipNewline() {
        if (nextChar == '\n') {
            lineNum++;
            nextChar = getChar();
            colNum = 1;
            return true;
        }
        if (nextChar == '\r') {
            lineNum++;
            nextChar = getChar();
            colNum = 1;
            // skip over next char if '\n'
            if (nextChar == '\n') {
                nextChar = getChar();
                colNum = 1;
            }
            return true;
        }
        return false;
    }

    // return the next token without "consuming" it
    public Token peek() {
        // advance token only if its been reset by nextTokenInternal()
        if (next == null)
            next = nextTokenInternal();

        return next;
    }

    // When called, return the next token (refer to the code "Token.java")
    // from the input stream.
    // Return TOKEN_EOF when reaching the end of the input stream.
    private Token nextTokenInternal() {
        // check if peek() was called
        if (next != null) {
            Token token = next;
            next = null; // allow peek to call for next token
            return token;
        }

        if (-1 == nextChar)
            return new Token(Kind.TOKEN_EOF, lineNum, colNum);

        // skip all kinds of "blanks"
        while (Character.isWhitespace(nextChar)) {
            // check if whitespace char is a newline
            if (!skipNewline()) {
                nextChar = getChar();
            }
            // offset colNum for tab chars
            if (nextChar == '\t')
                colNum += 3;
        }
        if (-1 == nextChar)
            return new Token(Kind.TOKEN_EOF, lineNum, colNum);

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
                return new Token(type, lineNum, colNum - idVal.length());
            else // token is an identifier
                return new Token(Kind.TOKEN_ID, lineNum, colNum - idVal.length(), idVal);
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
            return new Token(Kind.TOKEN_NUM, lineNum, colNum - numString.length(), numString);
        }

        if (nextChar == '/') {
            colNum++;
            nextChar = getChar();

            // single-line comment (skip to the next line)
            if (nextChar == '/') {
                do {
                    colNum++;
                    nextChar = getChar();
                } while (!skipNewline() && nextChar != -1);

                // grab next token
                return nextTokenInternal();
            }

            // multi-line comment (skip input until matching '*/' is found)
            if (nextChar == '*') {
                colNum++;
                nextChar = getChar();

                // keep track of nesting level
                int nestingLevel = 1;
                while (nestingLevel > 0) {
                    if (nextChar == '*') {
                        // check if it closes a comment
                        colNum++;
                        nextChar = getChar();

                        if (nextChar == '/') {
                            nestingLevel--;
                            colNum++;
                            nextChar = getChar();
                        }

                    } else if (nextChar == '/') {
                        // check if it starts a nested comment
                        colNum++;
                        nextChar = getChar();

                        if (nextChar == '*') {
                            nestingLevel++;
                            colNum++;
                            nextChar = getChar();
                        }

                    } else {
                        // check if EOF is reached before comment is terminated
                        if (nextChar == -1)
                            break;

                        // process newline chars
                        if (!skipNewline()) {
                            colNum++;
                            nextChar = getChar();
                        }

                    }
                }
                // grab next token
                return nextTokenInternal();
            }
        }

        // check for binOps
        switch (nextChar) {
            case '+':
                nextChar = getChar();
                return new Token(Kind.TOKEN_ADD, lineNum, colNum - 1);
            case '-':
                nextChar = getChar();
                return new Token(Kind.TOKEN_SUB, lineNum, colNum - 1);
            case '*':
                nextChar = getChar();
                return new Token(Kind.TOKEN_TIMES, lineNum, colNum - 1);
            case '<':
                nextChar = getChar();
                return new Token(Kind.TOKEN_LT, lineNum, colNum - 1);
            case '&':
                nextChar = getChar();
                // check if next char is '&' to match '&&' binop
                if (nextChar == '&') {
                    nextChar = getChar();
                    return new Token(Kind.TOKEN_AND, lineNum, colNum - 2);
                } else
                    return new Token(Kind.TOKEN_UNKNOWN, lineNum, colNum - 1);
        }

        // check for punctuation
        Kind type = punctuation.get((char) nextChar);
        nextChar = getChar();

        // found punctuation token
        if (type != null)
            return new Token(type, lineNum, colNum - 1);

        // token type is unknown
        return new Token(Kind.TOKEN_UNKNOWN, lineNum, colNum - 1);
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
