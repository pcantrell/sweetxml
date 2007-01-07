package net.innig.sweetxml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

class ConverterInput
    {
    private BufferedReader in;
    private String sourceName;
    private int line = 1, column = 0;
    private int markLine, markColumn;
    
    public ConverterInput(Reader reader, String sourceName)
        {
        this.in = (reader instanceof BufferedReader)
            ? (BufferedReader) reader
            : new BufferedReader(reader);
        this.sourceName = sourceName;
        }
    
    public InputPosition getPosition()
        { return new InputPosition(sourceName, line, column); }

    public int read() throws IOException
        {
        mark(1);
        return countChar(in.read());
        }

    public int peek() throws IOException
        { return skip('\0', false); }
    
    public int skip(char skippable, boolean count) throws IOException
        {
        mark(1);
        int c = in.read();
        if(c != skippable)
            reset();
        else if(count)
            c = countChar(c);
        return c;
        }

    public boolean lookingAt(String str)
        throws IOException
        {
        mark(str.length());
        for(char c : str.toCharArray())
            if(in.read() != c)
                {
                reset();
                return false;
                }
        return true;
        }

    private void mark(int count)
        throws IOException
        {
        in.mark(count);
        markLine = line;
        markColumn = column;
        }
    
    public void reset()
        throws IOException
        {
        in.reset();
        line = markLine;
        column = markColumn;
        }

    public int countChar(int c)
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
    }
