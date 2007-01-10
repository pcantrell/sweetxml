package net.innig.sweetxml;

import java.util.HashMap;
import java.util.Map;


/**
 * Represents an input / output format combination for conversion. 
 */
public enum ConversionMode
    {
    /**
     * Converion from XML to SweetXML.
     */
    X2S("xml", "sxml") {
        public Converter createConverter()
            { return new XmlToSweetConverter(); }
        },
        
    /**
     * Converion from SweetXML to XML.
     */
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
    
    /**
     * Returns the conversion mode which converts <i>from</i> files with the given extension.
     * @param ext A file extension, including the dot.
     * @throws IllegalArgumentException If no converter handles the given file extension.
     */
    public static ConversionMode forSourceExtension(String ext)
        { return bySourceExt.get(ext); }
        
    /**
     * Returns the conversion mode which converts <i>to</i> files with the given extension.
     * @param ext A file extension, including the dot.
     * @throws IllegalArgumentException If no converter handles the given file extension.
     */
    public static ConversionMode forTargetExtension(String ext)
        { return byTargetExt.get(ext); }
    
    // ---
        
    private final String sourceExt, targetExt;
    
    private ConversionMode(String sourceExt, String targetExt)
        {
        this.sourceExt = "." + sourceExt;
        this.targetExt = "." + targetExt;
        }
    
    /**
     * Creates a converter which operates in this mode.
     */
    public abstract Converter createConverter();

    /**
     * Returns the extension (including the dot) of file which this mode accepts as input.
     */
    public String getSourceExtension()
        { return sourceExt; }

    /**
     * Returns the extension (including the dot) of file which this mode produces as output.
     */
    public String getTargetExtension()
        { return targetExt; }
    }
