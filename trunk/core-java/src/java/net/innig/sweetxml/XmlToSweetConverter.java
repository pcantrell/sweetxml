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
            { this.in = getInput(); }

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
                readWhitespace(true);
                
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
            
            while(true)
                {
                readTagWhitespace(tagStart.getColumn() + 2);
                
                int c = in.read();
                if(c == -1)
                    throw new SweetXmlParseException(tagStart, EOF_IN_TAG, s2xName(name));
                else if(c == '>')
                    break;
                else if(c == '/' && in.lookingAt(">"))
                    {
                    tagEnded(name, tagStart);
                    break;
                    }
                else if(Patterns.charMatches(c, Patterns.xmlNameStartChar))
                    {
                    in.unread();
                    readAttribute(name, tagStart);
                    }
                else
                    throw new SweetXmlParseException(
                        in.getPosition(), UNEXPECTED_CHAR_IN_TAG, SweetXmlMessage.formatChar(c), s2xName(name));
                }
            
            indent++;
            }

        private void readAttribute(String tagName, DocumentPosition tagStart)
            throws IOException
            {
            String name = readName(tagStart, tagName);
            sxml.append(name);
            readTagWhitespace(tagStart.getColumn() + 2);
            
            int c = in.read();
            if(c != '=')
                throw new SweetXmlParseException(
                    in.getPosition(), EXPECTED_EQ_IN_XML_ATTRIBUTE,
                    SweetXmlMessage.formatChar(c), s2xName(tagName), s2xName(name));
            sxml.append('=');
            readTagWhitespace(tagStart.getColumn() + 2);
            
            c = in.read();
            if(c == -1)
                throw new SweetXmlParseException(tagStart, EOF_IN_TAG, s2xName(tagName));
            if(c == '\'' || c == '"')
                {
                int quote = c;
                StringBuilder text = new StringBuilder(32);
                while(true)
                    {
                    c = in.read();
                    if(c == -1)
                        throw new SweetXmlParseException(tagStart, EOF_IN_TAG, s2xName(tagName));
                    if(c == quote)
                        break;
                    text.append((char) c);
                    }
                printQuoted(text.toString(), true);
                }
            else
                throw new SweetXmlParseException(
                    in.getPosition(), EXPECTED_XML_ATTRIBUTE_VALUE,
                    SweetXmlMessage.formatChar(c), s2xName(tagName), s2xName(name));
            }
        
        private void readTagWhitespace(int leftTrim)
            throws IOException
            {
            CharSequence ws = readWhitespace(false, true);
            Matcher line = Patterns.line.matcher(ws);
            while(line.find())
                {
                String eol = line.group(1);
                String spaces = line.group(2).replace("\t", "    ");
                if(spaces.length() == 0 && eol.length() == 0)
                    break;
                
                if(eol.length() > 0)
                    {
                    sxml.append(newline);
                    indent();
                    sxml.append("| ");
                    spaces = (spaces.length() > leftTrim)
                        ? spaces.substring(leftTrim)
                        : "";
                    }
                sxml.append(spaces);
                }
            }

        private void readEndTag()
            throws IOException, SweetXmlParseException
            {
            flushText(true);
            DocumentPosition tagStart = in.getPosition();
            String name = readName(in.getPosition(), null);
            readWhitespace(false);
            int c = in.read();
            if(c != '>')
                throw new SweetXmlParseException(
                    in.getPosition(), UNEXPECTED_CHAR_IN_END_TAG, SweetXmlMessage.formatChar(c), s2xName(name));
            tagEnded(name, tagStart);
            }
        
        private void tagEnded(String name, DocumentPosition tagStart)
            throws IOException
            {
            if(tagStack.isEmpty())
                throw new SweetXmlParseException(tagStart, UNEXPECTED_END_TAG, s2xName(name));
            String popped = tagStack.removeLast();
            if(!popped.equals(name))
                throw new SweetXmlParseException(tagStart, MISMATCHED_END_TAG, s2xName(popped), s2xName(name));
            
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
                    throw new SweetXmlParseException(tagStart, EOF_IN_TAG, s2xName(tagName));
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
                    throw new SweetXmlParseException(tagStart, EOF_IN_TAG, s2xName((tagName != null) ? tagName : name));
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
        
        private String s2xName(CharSequence name)
            { return name.toString().replace('/', ':'); }
        
        private void readComment()
            throws IOException
            {
            flush();
            sxml.append("# ");
            boolean lineStarted = false;
            while(true)
                {
                int c = in.read();
                if(c == -1)
                    break;
                else if(c == '-' && in.lookingAt("->"))
                    break;
                else if(c == '\n')
                    {
                    sxml.append(newline);
                    indent();
                    sxml.append("# ");
                    lineStarted = false;
                    }
                else 
                    {
                    if(!lineStarted && Character.isSpaceChar(c))
                        continue;
                    lineStarted = true;
                    sxml.append((char) c);
                    }
                }
            lineFull = true;
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

        private void readWhitespace(boolean countBreaks)
            throws IOException
            { readWhitespace(countBreaks, false); }
        
        private CharSequence readWhitespace(boolean countBreaks, boolean returnWhitespace)
            throws IOException
            {
            StringBuilder spaces = returnWhitespace ? new StringBuilder(32) : null;
            int c;
            while(Character.isWhitespace(c = in.read()))
                {
                if(c == '\n' && countBreaks)
                    lineBreaksPending++;
                if(returnWhitespace)
                    spaces.append((char) c);
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