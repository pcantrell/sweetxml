package net.innig.sweetxml;

import static net.innig.sweetxml.Patterns.newline;

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

import org.xml.sax.InputSource;

/**
 * Converts SweetXML to XML. Accepts a variety of input sources (via the various constructors)
 * and output sources (via the <tt>getResult*()</tt> methods). Each instance of this class
 * converts at most one input source, though you can read the output repeatedly.
 * <p>
 * Typical usage:
 * <pre>
 *   mySaxParser.parse(
 *       new SweetToXmlConverter(myInput).getResultInputSource(),
 *       mySaxHandler);
 * </pre>
 */
public class SweetToXmlConverter
    extends Converter
    {
    private String xml;

    public SweetToXmlConverter()
        { }
    
    public SweetToXmlConverter(Reader in, String sourceName)
        { setInput(in, sourceName); }
    
    public SweetToXmlConverter(InputStream in, String sourceName)
        throws UnsupportedEncodingException
        { this(new InputStreamReader(in, "utf-8"), sourceName); }

    public SweetToXmlConverter(InputStream in, String encoding, String sourceName)
        throws UnsupportedEncodingException
        { this(new InputStreamReader(in, encoding), sourceName); }
    
    public SweetToXmlConverter(File document)
        throws UnsupportedEncodingException, FileNotFoundException
        { this(new FileInputStream(document), document.getPath()); }

    public SweetToXmlConverter(URL document)
        throws IOException
        { this(document.openStream(), document.toExternalForm()); }

    public InputSource getResultInputSource() throws IOException
        { return new InputSource(getResultReader()); }
    
    @Override
    protected String convert()
        throws IOException
        {
        return new Conversion().go().toString();
        }
        
    private class Conversion
        {
        private ConverterInput in;
        private StringBuilder xml;
        private LinkedList<String> tagStack, indentStack;
        private boolean indenting, insideTag;
        private StringBuilder indentWork;
        
        public Conversion()
            {
            this.in = new ConverterInput(getInput(), getSourceName())
                {
                public int countChar(int c)
                    throws IOException
                    {
                    c = super.countChar(c);
                    
                    if(c == '\n')
                        {
                        indentWork = new StringBuilder(64);
                        indenting = true;
                        }
                    else if(indenting)
                        {
                        if(Character.isWhitespace(c))
                            indentWork.append((char) c);
                        else
                            indenting = false;
                        }
                    
                    return c;
                    }
                };
            }

        public CharSequence go()
            throws IOException
            {
            xml = new StringBuilder(32768);
            tagStack = new LinkedList<String>();
            indentStack = new LinkedList<String>();
            indenting = true;
            indentWork = new StringBuilder();
            
            if(isHeaderIncluded())
                xml.append("<?xml version=\"1.0\"?>").append(newline);
            
            readProcessingDirectives();
            
            readLoop:
            while(true)
                {
                skipWhitespace(true);
                int c = in.peek();
                switch(c)
                    {
                    case -1:
                        break readLoop;
                    
                    case '#':
                        readComment();
                        break;
                    
                    case '"':
                    case '\'':
                        handleIndent();
                        in.read(); // skip the quote
                        readQuotedText(c);
                        break;
                        
                    case '|':
                        if(!insideTag)
                            throw new SweetXmlParseException(in.getPosition(),
                                "Found a tag continuation symbol (\"|\"), but no tag is open here");
                        in.read(); // skip the bar
                        readAttributes();
                        break;
                    
                    default:
                        handleIndent();
                        readTag();
                        break;
                    }
                }
            
            indentWork = new StringBuilder(0);
            handleIndent(); // close all tags
            
            return xml;
            }
        
        private void readProcessingDirectives() throws IOException
            {
            while(true)
                {
                skipWhitespace(true);
                if(in.peek() == '#')
                    {
                    readComment();
                    continue;
                    }
                if(!in.lookingAt("<!"))
                    break;
                in.reset();
                
                int nest = 0;
                while(true)
                    {
                    int c = in.read();
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
            finishTag();
            
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
                throw new SweetXmlParseException(
                    in.getPosition(),
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
            
            insideTag = true;
            String tagIndent = indentWork.toString();
            String tagName = readName();
            tagStack.addFirst(tagName);
            xml.append(newline).append(tagIndent);
            xml.append('<').append(tagName);
            
            readAttributes();
            }

        private void readAttributes()
            throws IOException, SweetXmlParseException
            {
            boolean subsequentText = false;
            while(true)
                {
                skipWhitespace(false);
                int c = in.read();
                if(c == ':')
                    subsequentText = true;
                if(c == '#')
                    in.reset();
                if(c == -1 || c == ':' || c == '\n' || c == '#')
                    break;
                in.reset();
                
                xml.append(' ').append(readName());
                skipWhitespace(false);
                c = in.read();
                if(c != '=')
                    throw new SweetXmlParseException(in.getPosition(), "expected '=' while parsing tag attributes, but got '" + (char) c + "'");
                skipWhitespace(false);
                xml.append("=\"");
                readText();
                xml.append('"');
                }
            if(subsequentText)
                {
                finishTag();
                skipWhitespace(false);
                readText();
                }
            }

        private void finishTag()
            {
            if(insideTag)
                {
                xml.append('>');
                insideTag = false;
                }
            }
    
        private void readText() throws IOException
            {
            int c = in.read();
            if(c == '"' || c == '\'')
                readQuotedText(c);
            else
                while(true)
                    {
                    if(c == -1 || Patterns.charMatches(c, Patterns.quotingRequired))
                        {
                        in.reset();
                        break;
                        }
                    xml.append((char) c);
                    c = in.read();
                    }
            }

        private void readQuotedText(int quoteChar) throws IOException
            {
            DocumentPosition quoteStart = in.getPosition();
            while(true)
                {
                int c = in.read();
                if(c == -1)
                    throw new SweetXmlParseException(quoteStart, "Unterminated quote");
                if(c == quoteChar)
                    return;
                if(c == '<')
                    xml.append("&lt;");
                else if(c == '>')
                    xml.append("&gt;");
                else if(c == quoteChar)
                    xml.append((c == '"') ? "&quot;" : "&apos;");
                else
                    xml.append((char) c);
                }
            }
    
        private void readComment() throws IOException
            {
            while(true)
                {
                int c = in.read();
                if(c == -1 || c == '\r' || c == '\n')
                    return;
                }
            }
    
        private String readName() throws IOException
            {
            StringBuilder name = new StringBuilder(32);
            int c;
            if(!isSxmlNameStartCharacter(c = in.read()))
                throw new SweetXmlParseException(in.getPosition(), "expected name, but found non-name character '" + (char)c + "'");
            name.append((char) c);
            while(isSxmlNameCharacter(c = in.read()))
                name.append((char) c);
            in.reset();
            return name.toString().replace('/', ':');
            }
        
        private boolean isSxmlNameStartCharacter(int c)
            { return Patterns.charMatches(c, Patterns.xmlNameStartChar); }
    
        private boolean isSxmlNameCharacter(int c)
            { return Patterns.charMatches(c, Patterns.xmlNameChar) || c == '/'; }
    
        private void skipWhitespace(boolean skipNewlines) throws IOException
            {
            int c;
            while(Character.isWhitespace(c = in.read()))
                if(c == '\n' && !skipNewlines)
                    break;
            in.reset();
            }
        
        private String explainIndent(String indent)
            {
            if(indent.length() == 0)
                return "no indentation";
            
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
