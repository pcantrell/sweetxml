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
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks positional information and provides look-ahead utilities during coversion.
 */
public class ConverterInput
    {
    private BufferedReader in;
    private String sourceName;
    private int line = 0, column = 0;
    private boolean eol = true, markEol;
    private int markLine, markColumn;
    private boolean unreadAllowed;
    private List<ConverterInputListener> listeners;
    
    /**
     * @param  input       The text to be converted.
     * @param  sourceName  An identifying string for the input to be used in error messages,
     *                     e.g. a filename.
     */
    public ConverterInput(Reader input, String sourceName)
        {
        this.in = (input instanceof BufferedReader)
            ? (BufferedReader) input
            : new BufferedReader(input);
        this.sourceName = sourceName;
        }
    
    public ConverterInput(InputStream in, String sourceName)
        throws UnsupportedEncodingException
        { this(new InputStreamReader(in, "utf-8"), sourceName); }

    public ConverterInput(InputStream in, String encoding, String sourceName)
        throws UnsupportedEncodingException
        { this(new InputStreamReader(in, encoding), sourceName); }
    
    public ConverterInput(File document)
        throws UnsupportedEncodingException, FileNotFoundException
        { this(new FileInputStream(document), document.getPath()); }

    public ConverterInput(URL document)
        throws IOException
        { this(document.openStream(), document.toExternalForm()); }
    
    public Reader getReader()
        { return in; }

    public String getSourceName()
        { return sourceName; }

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
        
        if(listeners != null)
            for(ConverterInputListener listener : listeners)
                listener.countChar(c);
        
        return c;
        }
    
    public void addListener(ConverterInputListener listener)
        {
        if(listeners == null)
            listeners = new ArrayList<ConverterInputListener>();
        listeners.add(listener);
        }

    public void close() throws IOException
        { in.close(); }

    }
