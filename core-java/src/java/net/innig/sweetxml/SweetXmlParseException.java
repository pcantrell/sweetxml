package net.innig.sweetxml;

import java.io.IOException;

/**
 * An error during a conversion to or from SweetXML.
 */
public class SweetXmlParseException
    extends IOException
    {
    private final DocumentPosition position;

    public SweetXmlParseException(DocumentPosition position, String message)
        {
        super(position + ": " + message);
        this.position = position;
        }
    
    public DocumentPosition getPosition()
        { return position; }
    }
