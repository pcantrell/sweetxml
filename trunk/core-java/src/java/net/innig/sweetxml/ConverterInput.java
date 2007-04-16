package net.innig.sweetxml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Tracks positional information and provides look-ahead utilities during coversion.
 */
class ConverterInput
    {
    private BufferedReader in;
    private String sourceName;
    private int line = 0, column = 0;
    private boolean eol = true, markEol;
    private int markLine, markColumn;
    private boolean unreadAllowed;
    
    public ConverterInput(Reader reader, String sourceName)
        {
        this.in = (reader instanceof BufferedReader)
            ? (BufferedReader) reader
            : new BufferedReader(reader);
        this.sourceName = sourceName;
        }
    
    public DocumentPosition getPosition()
        { return new DocumentPosition(sourceName, line, column); }

    public int read() throws IOException
        {
        mark(2);
        return countChar(in.read());
        }
    
    public int peek() throws IOException
        {
        int c = read();
        reset();
        return c;
        }
    
    public boolean lookingAt(String str)
        throws IOException
        {
        mark(str.length() * 2); // doubled because of CRLF -> LF conversion
        for(char c : str.toCharArray())
            if(countChar(in.read()) != c)
                {
                reset();
                return false;
                }
        return true;
        }

    public void unread()
        throws IOException
        {
        if(!unreadAllowed)
            throw new IllegalStateException("either unread() or peek() was called since the last read(), or read() was never called");
        reset();
        }

    private void mark(int count)
        throws IOException
        {
        unreadAllowed = true;
        in.mark(count);
        markEol = eol;
        markLine = line;
        markColumn = column;
        }
    
    private void reset()
        throws IOException
        {
        unreadAllowed = false;
        in.reset();
        eol = markEol;
        line = markLine;
        column = markColumn;
        }

    public int countChar(int c)
        throws IOException
        {
        if(eol)
            {
            line++;
            column = 0;
            eol = false;
            }
        
        if(c == '\r')
            {
            if(in.read() != '\n')  // \r\n is one line break, not two
                {
                in.reset();
                in.read();
                }
            c = '\n';              // normalize to UNIX line endings
            }
        
        if(c == '\n')
            eol = true;
        
        column++;
        
        return c;
        }
    }
