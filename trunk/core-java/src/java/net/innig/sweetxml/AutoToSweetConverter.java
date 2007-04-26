package net.innig.sweetxml;

public class AutoToSweetConverter
    extends AutoConverter
    {
    public AutoToSweetConverter()
        { }
    
    public AutoToSweetConverter(ConverterInput input)
        { setInput(input); }
    
    @Override
    protected Converter getSweetConverter()
        { return null; }

    @Override
    protected Converter getXmlConverter()
        { return new XmlToSweetConverter(); }
    }
