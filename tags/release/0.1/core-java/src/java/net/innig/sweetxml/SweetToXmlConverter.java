package net.innig.sweetxml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;

public class SweetToXmlConverter
    extends Converter
    {
    private static final Pattern
        xmlNameStartCharPat = Pattern.compile("[A-Za-z_/\\xC0-\\xD6\\xD8-\\xF6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]"),
        xmlNameCharPat      = Pattern.compile("[A-Za-z_/\\xC0-\\xD6\\xD8-\\xF6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-\\.0-9\\xB7\\u0300-\\u036F\\u203F-\\u2040]"),
        quotingRequiredPat  = Pattern.compile("[^A-Za-z0-9\\-\\./_]");
    private static String[] charEscapes = new String[128];
    static
        {
        charEscapes['<'] = "&lt;";
        charEscapes['>'] = "&gt;";
        charEscapes['"'] = "&quot;";
        charEscapes['\\'] = "&apos;"; //! is this right?
        }
    
    private String xml;
    private String newline = "\n";

    public SweetToXmlConverter()
        { }
    
    public SweetToXmlConverter(Reader in)
        { setInput(in); }
    
    public SweetToXmlConverter(InputStream in)
        throws UnsupportedEncodingException
        { this(new InputStreamReader(in, "utf-8")); }

    public SweetToXmlConverter(InputStream in, String encoding)
        throws UnsupportedEncodingException
        { this(new InputStreamReader(in, encoding)); }
    
    public SweetToXmlConverter(File document)
        throws UnsupportedEncodingException, FileNotFoundException
        { this(new FileInputStream(document)); }

    public SweetToXmlConverter(URL document)
        throws IOException
        { this(document.openStream()); }

    public InputSource getResultInputSource() throws IOException
        { return new InputSource(getResultReader()); }
    
    @Override
    protected String convert()
        throws IOException
        {
        return new Conversion(getInput()).go().toString();
        }
        
    private class Conversion
        {
        private BufferedReader in;
        private StringBuilder xml;
        private LinkedList<String> tagStack, indentStack;
        private boolean indenting;
        private StringBuilder indentWork;
        private int line, column;
        
        public Conversion(Reader in)
            {
            this.in = (in instanceof BufferedReader)
                ? (BufferedReader) in
                : new BufferedReader(in);
            }

        public CharSequence go()
            throws IOException
            {
            xml = new StringBuilder();
            tagStack = new LinkedList<String>();
            indentStack = new LinkedList<String>();
            indenting = true;
            indentWork = new StringBuilder();
            line = 1;
            column = 0;

            xml.append("<?xml version=\"1.0\"?>").append(newline);
            
            readProcessingDirectives();
            
            readLoop:
            while(true)
                {
                skipWhitespace(true);
                handleIndent();
                int c = peek();
                switch(c)
                    {
                    case -1:
                        break readLoop;
                    
                    case '#':
                        readComment();
                        break;
                    
                    case '"':
                    case '\'':
                        read(); // skip the quote
                        readQuotedText(c);
                        break;
                    
                    default:
                        readTag();
                        break;
                    }
                }
            
            indentWork = new StringBuilder();
            handleIndent(); // close all tags
            
            return xml;
            }
        
        private void readProcessingDirectives() throws IOException
            {
            while(true)
                {
                skipWhitespace(true);
                in.mark(2);
                boolean directive = (in.read() == '<' && in.read() == '!');
                in.reset();
                if(!directive)
                    break;
                
                int nest = 0;
                while(true)
                    {
                    int c = read();
                    switch(c)
                        {
                        case -1:
                            return;
                        case '<':
                            nest++;
                            break;
                        case '>':
                            nest--;
                            break;
                        }
                    xml.append((char) c);
                    if(nest == 0)
                        break;
                    }
                }
            }

        private void handleIndent() throws IOException
            {
            String indent = indentWork.toString();
            
            boolean first = true;
            while(!indentStack.isEmpty() && indentStack.getFirst().startsWith(indent))
                {
                String popped = indentStack.removeFirst();
                if(first)
                    first = false;
                else
                    xml.append(newline).append(popped);
                closeAndPopTag();
                }
            
            if(!indentStack.isEmpty() && !indent.startsWith(indentStack.getFirst()))
                throw new SweetXmlParseException(line, column,
                    "Inconsistent indentation: expected a line starting with "
                    + explainIndent(indentStack.getFirst()) + ", but got " + explainIndent(indent));
            
            indentStack.addFirst(indent);
            tagStack.addFirst(null);
            }

        private void closeAndPopTag()
            {
            String tag = tagStack.removeFirst();
            if(tag != null)
                xml.append("</").append(tag).append(">");
            }
        
        private void readTag() throws IOException
            {
            closeAndPopTag();
            
            String tagIndent = indentWork.toString();
            String tagName = readName();
            tagStack.addFirst(tagName);
            xml.append(newline).append(tagIndent);
            xml.append('<').append(tagName);
            
            boolean subsequentText = false;
            while(true)
                {
                skipWhitespace(false);
                int c = read();
                if(c == ':')
                    subsequentText = true;
                if(c == -1 || c == ':' || c == '\n')
                    break;
                backOne();
                
                xml.append(' ').append(readName());
                skipWhitespace(false);
                c = read();
                if(c != '=')
                    throw new SweetXmlParseException(line, column, "expected '=' while parsing tag attributes, but got '" + (char)c + "'");
                skipWhitespace(false);
                xml.append("=\"");
                readText();
                xml.append('"');
                }
            xml.append('>');
            if(subsequentText)
                {
                skipWhitespace(false);
                readText();
                }
            }
    
        private void readText() throws IOException
            {
            int c = read();
            if(c == '"' || c == '\'')
                readQuotedText(c);
            else
                while(true)
                    {
                    if(c == -1 || charMatches(c, quotingRequiredPat))
                        {
                        backOne();
                        break;
                        }
                    xml.append((char) c);
                    c = read();
                    }
            }

        private void readQuotedText(int quoteChar) throws IOException
            {
            int quoteStartLine = line;
            int quoteStartColumn = column;
            while(true)
                {
                int c = read();
                if(c == -1)
                    throw new SweetXmlParseException(quoteStartLine, quoteStartColumn, "Unterminated quote");
                if(c == '\\')
                    xml.append((char) read());
                if(c == quoteChar)
                    return;
                if(c < 128 && charEscapes[c] != null)
                    xml.append(charEscapes[c]);
                else
                    xml.append((char) c);
                }
            }
    
        private void readComment() throws IOException
            {
            while(true)
                {
                int c = read();
                if(c == -1 || c == '\r' || c == '\n')
                    return;
                }
            }
    
        private String readName() throws IOException
            {
            StringBuilder name = new StringBuilder();
            int c;
            if(!isXmlNameStartCharacter(c = read()))
                throw new SweetXmlParseException(line, column, "expected name, but found non-name character '" + (char)c + "'");
            name.append((char) c);
            while(isXmlNameCharacter(c = read()))
                name.append((char) c);
            backOne();
            return name.toString().replace('/', ':');
            }
        
        private boolean isXmlNameStartCharacter(int c)
            { return charMatches(c, xmlNameStartCharPat); }
    
        private boolean isXmlNameCharacter(int c)
            { return charMatches(c, xmlNameCharPat); }
    
        private void skipWhitespace(boolean skipNewlines) throws IOException
            {
            int c;
            while(Character.isWhitespace(c = read()))
                if(c == '\n' && !skipNewlines)
                    break;
            backOne();
            }
    
        private int read() throws IOException
            {
            in.mark(1);
            return countChar(in.read());
            }
    
        private int peek() throws IOException
            { return skip('\0', false); }
        
        private int skip(char skippable, boolean count) throws IOException
            {
            in.mark(1);
            int c = in.read();
            if(c != skippable)
                in.reset();
            else if(count)
                c = countChar(c);
            return c;
            }
    
        private void backOne() throws IOException
            {
            in.reset();
            if(--column < 0)
                line--;
            }

        private int countChar(int c)
            throws IOException
            {
            if(c == '\r')
                {
                skip('\n', false); // \r\n is one line break, not two
                c = '\n';          // normalize to UNIX line endings
                }
            
            if(c == '\n')
                {
                line++;
                column = 0;
                indentWork = new StringBuilder();
                indenting = true;
                }
            else
                {
                column++;
                if(indenting)
                    {
                    if(Character.isWhitespace(c))
                        indentWork.append((char) c);
                    else
                        indenting = false;
                    }
                }
            
            return c;
            }
        
        private boolean charMatches(int c, Pattern pat)
            { return pat.matcher(String.valueOf((char)c)).matches(); }

        private String explainIndent(String indent)
            {
            if(indent.length() == 0)
                return "the empty string";
            
            StringBuilder explanation = new StringBuilder();
            char curChar = indent.charAt(0);
            int curCount = 0;
            for(int i = 0; i <= indent.length(); i++)
                {
                int c = (i == indent.length()) ? -1 : indent.charAt(i);
                if(c != curChar)
                    {
                    if(explanation.length() > 0)
                        explanation.append(" + ");
                    explanation.append(curCount).append(' ');
                    if(curChar == ' ')
                        explanation.append("space");
                    else if(curChar == '\t')
                        explanation.append("tab");
                    else if(curChar <= 0xff)
                        explanation.append("\\x").append(Integer.toHexString(0x100 + curChar).substring(1));
                    else
                        explanation.append("\\u").append(Integer.toHexString(0x10000 + curChar).substring(1));
                    if(curCount > 1)
                        explanation.append("s");
                    
                    curChar = (char) c;
                    curCount = 0;
                    }
                curCount++;
                }
            
            return explanation.toString();
            }
        }
    }
