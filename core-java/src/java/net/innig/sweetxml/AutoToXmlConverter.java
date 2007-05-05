package net.innig.sweetxml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;


public class AutoToXmlConverter
    extends AutoConverter
    {
    public AutoToXmlConverter()
        { }
    
    public AutoToXmlConverter(ConverterInput input)
        { setInput(input); }
    
    public AutoToXmlConverter(Reader in, String sourceName)
        { this(new ConverterInput(in, sourceName)); }
        
    public AutoToXmlConverter(InputStream in, String sourceName)
        { this(new ConverterInput(in, sourceName)); }
    
    public AutoToXmlConverter(InputStream in, String encoding, String sourceName)
        throws UnsupportedEncodingException
        { this(new ConverterInput(in, encoding)); }
    
    public AutoToXmlConverter(File document)
        throws FileNotFoundException
        { this(new ConverterInput(document)); }
    
    public AutoToXmlConverter(URL document)
        throws IOException
        { this(new ConverterInput(document)); }
    
    @Override
    protected Converter getSweetConverter()
        { return new SweetToXmlConverter(); }

    @Override
    protected Converter getXmlConverter()
        { return null; }
    }
