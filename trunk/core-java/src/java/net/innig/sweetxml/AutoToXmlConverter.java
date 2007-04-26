package net.innig.sweetxml;


public class AutoToXmlConverter
    extends AutoConverter
    {
    public AutoToXmlConverter()
        { }
    
    public AutoToXmlConverter(ConverterInput input)
        { setInput(input); }
    
    @Override
    protected Converter getSweetConverter()
        { return new SweetToXmlConverter(); }

    @Override
    protected Converter getXmlConverter()
        { return null; }
    }
