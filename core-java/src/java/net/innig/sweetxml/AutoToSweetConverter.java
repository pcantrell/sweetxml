package net.innig.sweetxml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class AutoToSweetConverter
    extends AutoConverter
    {
    public AutoToSweetConverter()
        { }
    
    public AutoToSweetConverter(ConverterInput input)
        { setInput(input); }

    public AutoToSweetConverter(Reader in, String sourceName)
        { this(new ConverterInput(in, sourceName)); }
        
    public AutoToSweetConverter(InputStream in, String sourceName)
        { this(new ConverterInput(in, sourceName)); }
    
    public AutoToSweetConverter(InputStream in, String encoding, String sourceName)
        throws UnsupportedEncodingException
        { this(new ConverterInput(in, encoding)); }
    
    public AutoToSweetConverter(File document)
        throws FileNotFoundException
        { this(new ConverterInput(document)); }
    
    public AutoToSweetConverter(URL document)
        throws IOException
        { this(new ConverterInput(document)); }
    
    @Override
    protected Converter getSweetConverter()
        { return null; }

    @Override
    protected Converter getXmlConverter()
        { return new XmlToSweetConverter(); }
    }
