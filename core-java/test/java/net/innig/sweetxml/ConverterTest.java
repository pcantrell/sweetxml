package net.innig.sweetxml;

import java.io.IOException;
import java.io.StringReader;

import org.testng.ITest;


public abstract class ConverterTest
    implements ITest
    {
    private String name;
    private String input;
    private ConversionMode mode;
    
    public ConverterTest(
             String name,
             String input,
             ConversionMode mode)
        {
        this.name = name;
        this.input = input.trim();
        this.mode = mode;
        }
    
    public String getTestName()
        { return name; }

    protected String convert()
        throws IOException
        {
        Converter converter = mode.createConverter();
        converter.setInput(new StringReader(input.toString()), name);
        converter.setHeaderIncluded(false);
        String output = trim(converter.getResult());
        return output;
        }

    protected String trim(String s)
        {
        return s.trim().replaceAll("(\n|^) *(\n|$)", "$1$2");
        }

    protected String showInvisibles(String s)
        {
        return s.replaceAll(" ", "‧")
                .replaceAll("\t", "➞")
                .replaceAll("\n", "↩");
        }
    }
