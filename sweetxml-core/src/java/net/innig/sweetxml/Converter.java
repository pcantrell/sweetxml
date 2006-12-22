package net.innig.sweetxml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

public abstract class Converter
    {
    private Reader input;
    private String output;
    
    public synchronized void setInput(Reader input)
        { this.input = input; }
    
    public synchronized String getResult() throws IOException
        {
        if(output == null)
            {
            if(input == null)
                throw new IllegalStateException("must set input first");
            output = convert();
            input.close();
            input = null;
            }
        return output;
        }
        
    public Reader getResultReader() throws IOException
        { return new StringReader(getResult()); }
    
    public InputStream getResultInputStream() throws IOException
        { return new ByteArrayInputStream(getResult().getBytes()); }
    
    public InputStream getResultInputStream(String encoding) throws IOException
        { return new ByteArrayInputStream(getResult().getBytes(encoding)); }
    
    protected synchronized Reader getInput()
        { return input; }
    
    protected abstract String convert() throws IOException;
    }
