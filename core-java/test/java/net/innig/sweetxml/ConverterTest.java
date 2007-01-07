package net.innig.sweetxml;

import java.io.IOException;
import java.io.StringReader;

import org.testng.ITest;
import org.testng.annotations.Test;


public class ConverterTest
    implements ITest
    {
    private String name;
    private String input;
    private String expectedOutput;
    private ConversionMode mode;
    
    public ConverterTest(
             String name,
             String input,
             String expectedOutput,
             ConversionMode mode)
        {
        this.name = name;
        this.input = input.trim();
        this.expectedOutput = expectedOutput.trim();
        this.mode = mode;
        }
    
    public String getTestName()
        { return name; }
    
    @Test
    public void testConversion()
        throws IOException
        {
        Converter converter = mode.createConverter();
        converter.setInput(new StringReader(input.toString()));
        converter.setHeaderIncluded(false);
        String output = converter.getResult().trim();
        assert expectedOutput.equals(output)
            : "Mismatched conversion result."
                + "\n\nExpected:\n" + showInvisibles(expectedOutput)
                + "\n\nActual:\n" + showInvisibles(output);
        }

    private String showInvisibles(String s)
        {
        return s.replaceAll(" ", ".")
                .replaceAll("\t", "[tab]")
                .replaceAll("\n", "[CR]");
//        return s.replaceAll(" ", "‧")
//                .replaceAll("\t", "➞")
//                .replaceAll("\n", "↩");
        }
    }
