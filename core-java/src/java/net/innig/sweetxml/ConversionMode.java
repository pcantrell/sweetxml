package net.innig.sweetxml;

import java.util.HashMap;
import java.util.Map;


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
        
    // ---
    
    private static final Map<String,ConversionMode>
        bySourceExt = new HashMap<String,ConversionMode>(),
        byTargetExt = new HashMap<String,ConversionMode>();
    static
        {
        for(ConversionMode mode : values())
            {
            bySourceExt.put(mode.getSourceExtension(), mode);
            byTargetExt.put(mode.getTargetExtension(), mode);
            }
        }
    
    public static ConversionMode forSourceExtension(String ext)
        { return bySourceExt.get(ext); }
        
    public static ConversionMode forTargetExtension(String ext)
        { return byTargetExt.get(ext); }
    
    // ---
        
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
