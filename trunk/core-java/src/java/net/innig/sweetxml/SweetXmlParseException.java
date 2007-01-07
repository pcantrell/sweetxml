package net.innig.sweetxml;

import java.io.IOException;

public class SweetXmlParseException
    extends IOException
    {
    private final InputPosition position;

    public SweetXmlParseException(InputPosition position, String message)
        {
        super(position + ": " + message);
        this.position = position;
        }
    
    public InputPosition getPosition()
        { return position; }
    }
