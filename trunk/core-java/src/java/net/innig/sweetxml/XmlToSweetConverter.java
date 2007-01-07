package net.innig.sweetxml;

import static net.innig.sweetxml.Patterns.newline;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilderFactory;

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
        return new Conversion(getInput()).go().toString();
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
        
        public Conversion(Reader in)
            { this.in = new ConverterInput(in); }

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
                
                //! follwing code won't handle < or > inside comments or quotes
                int startLine = in.getLine(), startColumn = in.getColumn() + 1;
                if(in.lookingAt("<?"))
                    {
                    while(true)
                        {
                        int c = in.read();
                        if(c == '>')
                            break;
                        if(c == -1)
                            throw new SweetXmlParseException(startLine, startColumn, "XML declaration runs off end of file");
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
                            throw new SweetXmlParseException(startLine, startColumn, "Processing directive runs off end of file");
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
            
            int tagStartLine = in.getLine(), tagStartColumn = in.getColumn();
            
            String name = readName(tagStartLine, tagStartColumn);
            tagStack.addLast(name);
            sxml.append(name);
            indent++;
            
            while(true)
                {
                readWhitespace(true, false);
                
                int c = in.read();
                if(c == -1)
                    throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag <" + name + "> runs off end of file");
                else if(c == '>')
                    return;
                else if(c == '/' && in.lookingAt(">"))
                    {
                    tagEnded(name, tagStartLine, tagStartColumn);
                    return;
                    }
                else if(Patterns.charMatches(c, Patterns.xmlNameStartChar))
                    {
                    in.reset();
                    readAttribute(name, tagStartLine, tagStartColumn);
                    }
                else
                    throw new SweetXmlParseException(in.getLine(), in.getColumn(), "Unexpected character '" + (char) c + "' in tag <" + name + ">");
                }
            }
        
        private void readAttribute(String tagName, int tagStartLine, int tagStartColumn)
            throws IOException
            {
            String name = readName(tagStartLine, tagStartColumn);
            readWhitespace(true, false);
            if(!in.lookingAt("="))
                throw new SweetXmlParseException(in.getLine(), in.getColumn(), "Expected '=' but found '" + (char) in.read() + "' in tag <" + name + ">");
            readWhitespace(true, false);
            
            sxml.append(' ').append(name).append('=');
            
            int c = in.read();
            if(c == -1)
                throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag <" + name + "> runs off end of file");
            if(c == '\'' || c == '"')
                {
                int quote = c;
                StringBuilder text = new StringBuilder(32);
                while(true)
                    {
                    c = in.read();
                    if(c == -1)
                        throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag <" + name + "> runs off end of file");
                    if(c == quote)
                        break;
                    text.append((char) c);
                    }
                printQuoted(text.toString(), false);
                }
            else
                throw new SweetXmlParseException(in.getLine(), in.getColumn(), "Expected quoted attribute value, but found '" + (char) in.read() + "' in tag <" + name + ">");
            }

        private void readEndTag()
            throws IOException, SweetXmlParseException
            {
            flushText(true);
            int tagStartLine = in.getLine(), tagStartColumn = in.getColumn();
            String name = readName(in.getLine(), in.getColumn());
            readWhitespace(true, false);
            if(!in.lookingAt(">"))
                throw new SweetXmlParseException(in.getLine(), in.getColumn(), "Unexpected content in end tag </" + name + ">");
            tagEnded(name, tagStartLine, tagStartColumn);
            }
        
        private void tagEnded(String name, int tagStartLine, int tagStartColumn)
            throws IOException
            {
            if(tagStack.isEmpty())
                throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Unexpected closing tag </" + name + ">");
            String popped = tagStack.removeLast();
            if(!popped.equals(name))
                throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Mismatched closing tag: expected </" + popped + ">, but found </" + name + ">");
            
            if(onTagLine)
                lineFull = true;
            
            indent--;
            if(lineBreaksPending > 0)
                lineBreaksPending--;
            }

        private String readName(int tagStartLine, int tagStartColumn)
            throws IOException
            {
            StringBuilder name = new StringBuilder(16);
            
            int c = in.read();
            if(c == -1)
                throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag runs off end of file");
            if(!Patterns.charMatches(c, Patterns.xmlNameStartChar))
                throw new SweetXmlParseException(in.getLine(), in.getColumn(), "Unexpected character in tag: '" + (char) c + '\'');
            name.append((char) c);
            
            while(true)
                {
                c = in.read();
                if(c == -1)
                    throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag runs off end of file");
                if(c == ':')
                    name.append('/');
                else if(Patterns.charMatches(c, Patterns.xmlNameChar))
                    name.append((char) c);
                else
                    break;
                }
            in.reset();
            
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
            in.reset();
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