package net.innig.sweetxml;


public enum ConversionMode
    {
    X2S("xml", "sxml") {
        public Converter createConverter()
            { return new XmlToSweetConverter(); }
        },
        
    S2X("sxml", "xml") {
        public Converter createConverter()
            { return new SweetToXmlConverter(); }
        };
        
    private final String sourceExt, targetExt;
    
    private ConversionMode(String sourceExt, String targetExt)
        {
        this.sourceExt = "." + sourceExt;
        this.targetExt = "." + targetExt;
        }
    
    public abstract Converter createConverter();

    public String getSourceExtension()
        { return sourceExt; }

    public String getTargetExtension()
        { return targetExt; }
    }
