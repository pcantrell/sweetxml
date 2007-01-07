package net.innig.sweetxml;

import static net.innig.sweetxml.Patterns.newline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        private BufferedReader in;
        private StringBuilder sxml;
        private LinkedList<String> tagStack;
        private int line, column;
        
        private int indent;
        private int lineBreaksPending;
        private StringBuilder text;
        private boolean insideTag, onTagLine;
        
        public Conversion(Reader in)
            {
            this.in = (in instanceof BufferedReader)
                ? (BufferedReader) in
                : new BufferedReader(in);
            }

        public CharSequence go()
            throws IOException
            {
            sxml = new StringBuilder(32768);
            tagStack = new LinkedList<String>();
            text = new StringBuilder();
            line = 1;
            column = 0;

            readProcessingDirectives();
            
            readLoop:
            while(true)
                {
                int c = read();
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
            
            flushText();
            flushLineBreaks();
            return sxml;
            }

        private void readProcessingDirectives()
            throws IOException
            {
            while(true)
                {
                readWhitespace(true, true);
                
                //! follwing code won't handle < or > inside comments or quotes
                int startLine = line, startColumn = column + 1;
                if(lookingAt("<?"))
                    {
                    while(true)
                        {
                        int c = read();
                        if(c == '>')
                            break;
                        if(c == -1)
                            throw new SweetXmlParseException(startLine, startColumn, "XML declaration runs off end of file");
                        }
                    }
                else if(lookingAt("<!--"))
                    readComment();
                else if(lookingAt("<!"))
                    {
                    flushLineBreaks();
                    sxml.append("<!");
                    for(int nest = 1; nest > 0; )
                        {
                        int c = read();
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
            if(lookingAt("!--"))
                readComment();
            else if(lookingAt("![CDATA["))
                readCData();
            else if(lookingAt("/"))
                readEndTag();
            else
                readStartOrEmptyTag();
            }

        private void readStartOrEmptyTag()
            throws IOException
            {
            flushText();
            flushLineBreaks();
            if(onTagLine)
                {
                sxml.append(newline);
                indent();
                }
            
            insideTag = true;
            onTagLine = true;
            
            int tagStartLine = line, tagStartColumn = column;
            
            String name = readName(tagStartLine, tagStartColumn);
            tagStack.addLast(name);
            sxml.append(name);
            indent++;
            
            while(true)
                {
                readWhitespace(true, false);
                
                int c = read();
                if(c == -1)
                    throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag <" + name + "> runs off end of file");
                else if(c == '>')
                    return;
                else if(c == '/' && lookingAt(">"))
                    {
                    tagEnded(name, tagStartLine, tagStartColumn);
                    return;
                    }
                else if(charMatches(c, Patterns.xmlNameStartChar))
                    {
                    backOne();
                    readAttribute(name, tagStartLine, tagStartColumn);
                    }
                else
                    throw new SweetXmlParseException(line, column, "Unexpected character '" + (char) c + "' in tag <" + name + ">");
                }
            }
        
        private void readAttribute(String tagName, int tagStartLine, int tagStartColumn)
            throws IOException
            {
            String name = readName(tagStartLine, tagStartColumn);
            readWhitespace(true, false);
            if(!lookingAt("="))
                throw new SweetXmlParseException(line, column, "Expected '=' but found '" + (char) read() + "' in tag <" + name + ">");
            readWhitespace(true, false);
            
            sxml.append(' ').append(name).append('=');
            
            int c = read();
            if(c == -1)
                throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag <" + name + "> runs off end of file");
            if(c == '\'' || c == '"')
                {
                int quote = c;
                StringBuilder text = new StringBuilder(32);
                while(true)
                    {
                    c = read();
                    if(c == -1)
                        throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag <" + name + "> runs off end of file");
                    if(c == quote)
                        break;
                    text.append((char) c);
                    }
                printQuoted(text.toString(), false);
                }
            else
                throw new SweetXmlParseException(line, column, "Expected quoted attribute value, but found '" + (char) read() + "' in tag <" + name + ">");
            }

        private void readEndTag()
            throws IOException, SweetXmlParseException
            {
            flushText();
            int tagStartLine = line, tagStartColumn = column;
            String name = readName(line, column);
            readWhitespace(true, false);
            if(!lookingAt(">"))
                throw new SweetXmlParseException(line, column, "Unexpected content in end tag </" + name + ">");
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
            
            indent--;
            if(lineBreaksPending > 0)
                lineBreaksPending--;
            }

        private String readName(int tagStartLine, int tagStartColumn)
            throws IOException
            {
            StringBuilder name = new StringBuilder(16);
            
            int c = read();
            if(c == -1)
                throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag runs off end of file");
            if(!charMatches(c, Patterns.xmlNameStartChar))
                throw new SweetXmlParseException(line, column, "Unexpected character in tag: '" + (char) c + '\'');
            name.append((char) c);
            
            while(true)
                {
                c = read();
                if(c == -1)
                    throw new SweetXmlParseException(tagStartLine, tagStartColumn, "Tag runs off end of file");
                if(c == ':')
                    name.append('/');
                else if(charMatches(c, Patterns.xmlNameChar))
                    name.append((char) c);
                else
                    break;
                }
            backOne();
            
            return name.toString();
            }
        
        private void readComment()
            throws IOException
            {
            flushText();
            flushLineBreaks();
            sxml.append('#');
            boolean hashed = true;
            while(true)
                {
                int c = read();
                if(c == -1)
                    return;
                else if(c == '-' && lookingAt("->"))
                    return;
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
            }
        
        private void readCData()
            throws IOException
            {
            while(true)
                {
                int c = read();
                if(c == -1)
                    return;
                if(c == ']' && lookingAt("]>"))
                    return;
                text.append((char) c);
                }
            }
        
        private CharSequence readWhitespace(boolean includeNewlines, boolean countBreaks)
            throws IOException
            {
            StringBuilder spaces = new StringBuilder(32);
            int c;
            while(Character.isWhitespace(c = read()))
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
            backOne();
            return spaces;
            }
        
        private void indent()
            {
            for(int n = indent; n > 0; n--)
                sxml.append("    ");
            }
        
        //! several of these are refactorable
        
        private int read() throws IOException
            {
            in.mark(1);
            return countChar(in.read());
            }
    
        private int peek() throws IOException
            { return skip('\0', false); }
        
        private boolean charMatches(int c, Pattern pat)
            { return pat.matcher(String.valueOf((char) c)).matches(); }

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
                }
            else
                column++;
            
            return c;
            }
        
        private boolean lookingAt(String str)
            throws IOException
            {
            in.mark(str.length());
            for(char c : str.toCharArray())
                if(in.read() != c)
                    {
                    in.reset();
                    return false;
                    }
            return true;
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
        
        private void flushText()
            {
            Matcher space = Patterns.trimWhitespace.matcher(text);
            space.find();
            
            for(Matcher m = Patterns.lineBreak.matcher(space.group(1)); m.find(); )
                lineBreaksPending++;
            
            if(space.group(2).length() > 0)
                {
                flushLineBreaks();
                if(insideTag)
                    sxml.append(": ");
                insideTag = false;
                printQuoted(space.group(2), !onTagLine);
                }
            
            text = new StringBuilder();
            }
        
        private void flushLineBreaks()
            {
            while(lineBreaksPending-- > 0)
                {
                insideTag = onTagLine = false;
                sxml.append('\n');
                indent();
                }
            lineBreaksPending = 0;
            }
        }
    }