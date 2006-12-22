package net.innig.sweetxml;

import java.io.IOException;

public class SweetXmlParseException
    extends IOException
    {
    public SweetXmlParseException(int line, int column, String message)
        {
        super("Error on line " + line + " column " + column + ": " + message);
        }
    }
