package net.innig.sweetxml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

/**
 * A text format converter.
 */
public abstract class Converter
    {
    private Reader input;
    private String output;
    private String sourceName;
    private boolean header = true;
    
    /**
     * Sets the input source for this converter.
     * @param  input       The text to be converted.
     * @param  sourceName  An identifying string for the input to be used in error messages,
     *                  e.g. a filename.
     * @throws IllegalStateException If the input has already been set.
     */
    public synchronized void setInput(Reader input, String sourceName)
        {
        if(this.input != null)
            throw new IllegalStateException("input source already set");
        
        this.input = input;
        this.sourceName = sourceName;
        }

    /**
     * Returns the result of the conversion as a string.
     * Can be called multiple times, but will only perform the conversion once.
     */
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
        
    /**
     * Returns a character stream view of the conversion results.
     * Can be called multiple times, but will only perform the conversion once.
     */
    public Reader getResultReader() throws IOException
        { return new StringReader(getResult()); }
    
    /**
     * Returns a byte stream view of the conversion results, encoded using UTF-8.
     * Can be called multiple times, but will only perform the conversion once.
     */
    public InputStream getResultInputStream() throws IOException
        { return new ByteArrayInputStream(getResult().getBytes("UTF-8")); }
    
    /**
     * Returns a byte stream view of the conversion results using the given encoding.
     * Can be called multiple times, but will only perform the conversion once.
     */
    public InputStream getResultInputStream(String encoding) throws IOException
        { return new ByteArrayInputStream(getResult().getBytes(encoding)); }
    
    /**
     * Determines whether the output incldues a document header. (e.g. &lt;?xml version="1.0"?&gt;)
     */
    public boolean isHeaderIncluded()
        { return header; }

    public void setHeaderIncluded(boolean header)
        { this.header = header; }
    
    /**
     * Used by implementations during conversion.
     */
    protected final synchronized Reader getInput()
        { return input; }
    
    /**
     * Used by implementations during conversion.
     */
    protected final synchronized String getSourceName()
        { return sourceName; }
    
    /**
     * Implementations override this to perform the conversion. The method will be called
     * at most one time on a given object.
     */
    protected abstract String convert() throws IOException;
    }
