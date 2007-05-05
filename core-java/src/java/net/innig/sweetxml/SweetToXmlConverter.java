package net.innig.sweetxml;

import static net.innig.sweetxml.Patterns.newline;
import static net.innig.sweetxml.SweetXmlMessage.CONTINUATION_OUTSIDE_TAG;
import static net.innig.sweetxml.SweetXmlMessage.ENDLESS_QUOTE;
import static net.innig.sweetxml.SweetXmlMessage.EXPECTED_EQ_IN_ATTRIBUTE;
import static net.innig.sweetxml.SweetXmlMessage.ILLEGAL_NAME_CHARACTER;
import static net.innig.sweetxml.SweetXmlMessage.INCONSISTENT_INDENTATION;
import static net.innig.sweetxml.SweetXmlMessage.INDENT_SPACES;
import static net.innig.sweetxml.SweetXmlMessage.INDENT_TABS;
import static net.innig.sweetxml.SweetXmlMessage.INDENT_UNICODE;
import static net.innig.sweetxml.SweetXmlMessage.MISPLACED_CONTINUATION;
import static net.innig.sweetxml.SweetXmlMessage.SEVERED_ATTRIBUTE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
    
    public SweetToXmlConverter(ConverterInput input)
        { setInput(input); }

    public SweetToXmlConverter(Reader in, String sourceName)
        { this(new ConverterInput(in, sourceName)); }
        
    public SweetToXmlConverter(InputStream in, String sourceName)
        { this(new ConverterInput(in, sourceName)); }
    
    public SweetToXmlConverter(InputStream in, String encoding, String sourceName)
        throws UnsupportedEncodingException
        { this(new ConverterInput(in, encoding)); }
    
    public SweetToXmlConverter(File document)
        throws FileNotFoundException
        { this(new ConverterInput(document)); }
    
    public SweetToXmlConverter(URL document)
        throws IOException
        { this(new ConverterInput(document)); }
    
    public InputSource getResultInputSource() throws IOException
        { return new InputSource(getResultReader()); }
    
    @Override
    protected String convert()
        throws IOException
        {
        return new Conversion().go().toString();
        }
        
    private class Conversion
        implements ConverterInputListener
        {
        private ConverterInput in;
        private StringBuilder xml;
        private LinkedList<String> tagStack, indentStack;
        private boolean indenting, insideTag;
        private StringBuilder indentWork;
        
        public Conversion()
            {
            in = getInput();
            in.addListener(this);
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
                        readQuotedText(c, false);
                        break;
                        
                    case '|':
                        in.read(); // skip the bar
                        if(!insideTag)
                            throw new SweetXmlParseException(in.getPosition(), CONTINUATION_OUTSIDE_TAG);
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
            boolean first = true;
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
                in.unread();
                
                if(first)
                    first = false;
                else
                    xml.append("\n");
                
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
                    INCONSISTENT_INDENTATION,
                    explainIndent(indentStack.getFirst()),
                    explainIndent(indent));
            
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
                    in.unread();
                if(c == -1 || c == ':' || c == '\n' || c == '#')
                    break;
                in.unread();
                
                String attrName = readName();
                xml.append(' ').append(attrName);
                skipAttributeInternalWhitespace(attrName);
                c = in.read();
                if(c != '=')
                    throw new SweetXmlParseException(
                        in.getPosition(), EXPECTED_EQ_IN_ATTRIBUTE, SweetXmlMessage.formatChar(c));
                skipAttributeInternalWhitespace(attrName);
                xml.append("=\"");
                readText(true);
                xml.append('"');
                }
            if(subsequentText)
                {
                finishTag();
                skipWhitespace(false);
                readText(false);
                }
            }
        
        private void skipAttributeInternalWhitespace(String attrName) throws IOException
            {
            in.read();
            DocumentPosition startPosition = in.getPosition();
            in.unread();
            boolean continuationAllowed = false, continued = true;
            while(true)
                {
                skipWhitespace(false);
                switch(in.read())
                    {
                    case '\n':
                        continuationAllowed = true;
                        continued = false;
                        break;
                        
                    case '|':
                        if(!continuationAllowed)
                            throw new SweetXmlParseException(in.getPosition(), MISPLACED_CONTINUATION);
                        continuationAllowed = false;
                        continued = true;
                        break;
                    
                    case -1:
                        continued = false;
                    default:
                        if(!continued)
                            throw new SweetXmlParseException(startPosition, SEVERED_ATTRIBUTE, attrName);
                        in.unread();
                        return;
                    }
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
    
        private void readText(boolean escapeQuot) throws IOException
            {
            int c = in.read();
            if(c == '"' || c == '\'')
                readQuotedText(c, escapeQuot);
            else
                while(true)
                    {
                    if(c == -1 || Patterns.charMatches(c, Patterns.quotingRequired))
                        {
                        in.unread();
                        break;
                        }
                    xml.append((char) c);
                    c = in.read();
                    }
            }

        private void readQuotedText(int quoteChar, boolean escapeQuot) throws IOException
            {
            DocumentPosition quoteStart = in.getPosition();
            while(true)
                {
                int c = in.read();
                if(c == -1)
                    throw new SweetXmlParseException(quoteStart, ENDLESS_QUOTE);
                if(c == quoteChar)
                    return;
                if(c == '<')
                    xml.append("&lt;");
                else if(c == '>')
                    xml.append("&gt;");
                else if(c == '"' && escapeQuot)
                    xml.append("&quot;");
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
                throw new SweetXmlParseException(
                    in.getPosition(), ILLEGAL_NAME_CHARACTER, SweetXmlMessage.formatChar(c));
            name.append((char) c);
            while(isSxmlNameCharacter(c = in.read()))
                name.append((char) c);
            in.unread();
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
                {
                if(c == '\n' && !skipNewlines)
                    break;
                }
            in.unread();
            }
        
        public void countChar(int c)
            {
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
            }
        
        private String explainIndent(String indent)
            {
            if(indent.length() == 0)
                throw new IllegalArgumentException("explainIndent() called with empty indent string");
            
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
                    SweetXmlMessage charMsg;
                    if(curChar == ' ')
                        charMsg = INDENT_SPACES;
                    else if(curChar == '\t')
                        charMsg = INDENT_TABS;
                    else
                        charMsg = INDENT_UNICODE;
                    explanation.append(
                        charMsg.format(
                            curCount,
                            SweetXmlMessage.formatChar(curChar)));
                    
                    curChar = (char) c;
                    curCount = 0;
                    }
                curCount++;
                }
            
            return explanation.toString();
            }
        }
    }
