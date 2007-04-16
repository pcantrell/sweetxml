package net.innig.sweetxml;

import static net.innig.sweetxml.Patterns.newline;
import static net.innig.sweetxml.SweetXmlMessage.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Converts XML to SweetXML, attempting to preserve formatting where it makes sense.
 */
public class XmlToSweetConverter
    extends Converter
    {
    private DocumentBuilderFactory parserFactory;
    
    public XmlToSweetConverter()
        {
        parserFactory = DocumentBuilderFactory.newInstance();
        parserFactory.setValidating(false);
        parserFactory.setNamespaceAware(false);
        }

    protected String convert()
        throws IOException
        {
        return new Conversion().go().toString();
        }
    
    private class Conversion
        {
        private ConverterInput in;
        private StringBuilder sxml;
        private LinkedList<String> tagStack;
        
        private int indent;
        private int lineBreaksPending;
        private StringBuilder text;
        private boolean insideTag, onTagLine, lineFull;
        
        public Conversion()
            { this.in = new ConverterInput(getInput(), getSourceName()); }

        public CharSequence go()
            throws IOException
            {
            sxml = new StringBuilder(32768);
            tagStack = new LinkedList<String>();
            text = new StringBuilder();

            readProlog();
            
            readLoop:
            while(true)
                {
                int c = in.read();
                switch(c)
                    {
                    case -1:
                        break readLoop;
                    
                    case '<':
                        readElement();
                        break;
                    
                    default:
                        text.append((char) c);
                        break;
                    }
                }
            
            flush();
            return sxml;
            }

        private void readProlog()
            throws IOException
            {
            while(true)
                {
                readWhitespace(true, true);
                
                in.read();
                DocumentPosition start = in.getPosition();
                in.unread();
                
                //! follwing code won't handle < or > inside comments or quotes
                if(in.lookingAt("<?"))
                    {
                    while(true)
                        {
                        int c = in.read();
                        if(c == '>')
                            break;
                        if(c == -1)
                            throw new SweetXmlParseException(start, EOF_IN_XML_DECLARATION);
                        }
                    }
                else if(in.lookingAt("<!--"))
                    readComment();
                else if(in.lookingAt("<!DOCTYPE"))
                    {
                    flushLineBreaks();
                    sxml.append("<!DOCTYPE");
                    for(int nest = 1; nest > 0; )
                        {
                        int c = in.read();
                        if(c == -1)
                            throw new SweetXmlParseException(start, EOF_IN_DIRECTIVE);
                        sxml.append((char) c);
                        if(c == '<')
                            nest++;
                        if(c == '>')
                            nest--;
                        }
                    }
                else
                    return;
                }
            }
        
        private void readElement()
            throws IOException
            {
            if(in.lookingAt("!--"))
                readComment();
            else if(in.lookingAt("![CDATA["))
                readCData();
            else if(in.lookingAt("/"))
                readEndTag();
            else
                readStartOrEmptyTag();
            }

        private void readStartOrEmptyTag()
            throws IOException
            {
            flush();
            if(onTagLine)
                {
                sxml.append(newline);
                indent();
                }
            
            insideTag = true;
            onTagLine = true;
            
            DocumentPosition tagStart = in.getPosition();
            
            String name = readName(tagStart, null);
            tagStack.addLast(name);
            sxml.append(name);
            indent++;
            
            while(true)
                {
                readWhitespace(true, false);
                
                int c = in.read();
                if(c == -1)
                    throw new SweetXmlParseException(tagStart, EOF_IN_TAG, name);
                else if(c == '>')
                    return;
                else if(c == '/' && in.lookingAt(">"))
                    {
                    tagEnded(name, tagStart);
                    return;
                    }
                else if(Patterns.charMatches(c, Patterns.xmlNameStartChar))
                    {
                    in.unread();
                    readAttribute(name, tagStart);
                    }
                else
                    throw new SweetXmlParseException(
                        in.getPosition(), UNEXPECTED_CHAR_IN_TAG, SweetXmlMessage.formatChar(c), name);
                }
            }
        
        private void readAttribute(String tagName, DocumentPosition tagStart)
            throws IOException
            {
            String name = readName(tagStart, tagName);
            readWhitespace(true, false);
            int c = in.read();
            if(c != '=')
                throw new SweetXmlParseException(
                    in.getPosition(), EXPECTED_EQ_IN_XML_ATTRIBUTE, SweetXmlMessage.formatChar(c), tagName, name);
            readWhitespace(true, false);
            
            sxml.append(' ').append(name).append('=');
            
            c = in.read();
            if(c == -1)
                throw new SweetXmlParseException(tagStart, EOF_IN_TAG, tagName);
            if(c == '\'' || c == '"')
                {
                int quote = c;
                StringBuilder text = new StringBuilder(32);
                while(true)
                    {
                    c = in.read();
                    if(c == -1)
                        throw new SweetXmlParseException(tagStart, EOF_IN_TAG, tagName);
                    if(c == quote)
                        break;
                    text.append((char) c);
                    }
                printQuoted(text.toString(), false);
                }
            else
                throw new SweetXmlParseException(
                    in.getPosition(), EXPECTED_XML_ATTRIBUTE_VALUE,
                    SweetXmlMessage.formatChar(c), tagName, name);
            }

        private void readEndTag()
            throws IOException, SweetXmlParseException
            {
            flushText(true);
            DocumentPosition tagStart = in.getPosition();
            String name = readName(in.getPosition(), null);
            readWhitespace(true, false);
            int c = in.read();
            if(c != '>')
                throw new SweetXmlParseException(
                    in.getPosition(), UNEXPECTED_CHAR_IN_END_TAG, SweetXmlMessage.formatChar(c), name);
            tagEnded(name, tagStart);
            }
        
        private void tagEnded(String name, DocumentPosition tagStart)
            throws IOException
            {
            if(tagStack.isEmpty())
                throw new SweetXmlParseException(tagStart, UNEXPECTED_END_TAG, name);
            String popped = tagStack.removeLast();
            if(!popped.equals(name))
                throw new SweetXmlParseException(tagStart, MISMATCHED_END_TAG, popped, name);
            
            if(onTagLine)
                lineFull = true;
            
            indent--;
            if(lineBreaksPending > 0)
                lineBreaksPending--;
            }

        private String readName(DocumentPosition tagStart, String tagName)
            throws IOException
            {
            StringBuilder name = new StringBuilder(16);
            
            int c = in.read();
            if(c == -1)
                {
                if(tagName != null)
                    throw new SweetXmlParseException(tagStart, EOF_IN_TAG, tagName);
                else
                    throw new SweetXmlParseException(tagStart, EOF_IN_ANONYMOUS_TAG);
                }
            if(!Patterns.charMatches(c, Patterns.xmlNameStartChar))
                throw new SweetXmlParseException(
                    in.getPosition(), UNEXPECTED_CHAR_IN_ANONYMOUS_TAG, SweetXmlMessage.formatChar(c));
            name.append((char) c);
            
            while(true)
                {
                c = in.read();
                if(c == -1)
                    {
                    if(tagName != null)
                        throw new SweetXmlParseException(tagStart, EOF_IN_TAG, tagName);
                    else if(name.length() > 0)
                        throw new SweetXmlParseException(tagStart, EOF_IN_TAG, name);
                    else
                        throw new SweetXmlParseException(tagStart, EOF_IN_ANONYMOUS_TAG);
                    }
                if(c == ':')
                    name.append('/');
                else if(Patterns.charMatches(c, Patterns.xmlNameChar))
                    name.append((char) c);
                else
                    break;
                }
            in.unread();
            
            return name.toString();
            }
        
        private void readComment()
            throws IOException
            {
            flush();
            sxml.append('#');
            boolean hashed = true;
            while(true)
                {
                int c = in.read();
                if(c == -1)
                    break;
                else if(c == '-' && in.lookingAt("->"))
                    break;
                else if(c == '\n')
                    {
                    if(!hashed)
                        {
                        indent();
                        sxml.append("# ");
                        }
                    sxml.append(newline);
                    hashed = false;
                    }
                else 
                    {
                    if(!Character.isSpaceChar(c) && !hashed)
                        {
                        indent();
                        sxml.append("# ");
                        hashed = true;
                        }
                    if(hashed)
                        sxml.append((char) c);
                    }
                }
            lineFull = hashed;
            }
        
        private void readCData()
            throws IOException
            {
            flush();
            while(true)
                {
                int c = in.read();
                if(c == -1)
                    break;
                if(c == ']' && in.lookingAt("]>"))
                    break;
                if(c == '&')
                    text.append("&amp;");
                else
                    text.append((char) c);
                }
            flushText(false);
            }

        private CharSequence readWhitespace(boolean includeNewlines, boolean countBreaks)
            throws IOException
            {
            StringBuilder spaces = new StringBuilder(32);
            int c;
            while(Character.isWhitespace(c = in.read()))
                {
                if(c == '\n')
                    {
                    if(!includeNewlines)
                        break;
                    if(countBreaks)
                        lineBreaksPending++;
                    }
                spaces.append(c);
                }
            in.unread();
            return spaces;
            }
        
        private void indent()
            {
            for(int n = indent; n > 0; n--)
                sxml.append("    ");
            }
        
        private void printQuoted(String s, boolean forceQuotes)
            {
            if(!forceQuotes && !Patterns.quotingRequired.matcher(s).find())
                sxml.append(s);
            else if(s.contains("'") || !s.contains("\""))
                sxml.append("\"").append(s.replace("\"", "&quot;")).append("\"");
            else
                sxml.append("'").append(s).append("'");
            }
        
        private void flush()
            {
            flushText(true);
            flushLineBreaks();
            }
        
        private void flushText(boolean trim)
            {
            String content, spaceBefore = "";
            int breaksBefore = 0;
            if(trim)
                {
                Matcher space = Patterns.trimWhitespace.matcher(text);
                space.find();
                spaceBefore = space.group(1);
                content = space.group(2);
                
                for(Matcher m = Patterns.lineBreak.matcher(spaceBefore); m.find(); )
                    breaksBefore++;
                lineBreaksPending += breaksBefore;
                }
            else
                content = text.toString();
            
            if(content.length() > 0)
                {
                flushLineBreaks();
                if(insideTag)
                    {
                    sxml.append(": ");
                    if(breaksBefore == 0)
                        sxml.append(spaceBefore);
                    }
                insideTag = false;
                printQuoted(content, !onTagLine);
                lineFull = true;
                }
            
            text = new StringBuilder();
            }
        
        private void flushLineBreaks()
            {
            if(lineFull && lineBreaksPending <= 0)
                lineBreaksPending = 1;
            while(lineBreaksPending-- > 0)
                {
                lineFull = insideTag = onTagLine = false;
                sxml.append('\n');
                indent();
                }
            lineBreaksPending = 0;
            }
        }
    }