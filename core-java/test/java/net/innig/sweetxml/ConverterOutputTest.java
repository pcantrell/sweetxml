package net.innig.sweetxml;

import java.io.IOException;

import org.testng.annotations.Test;

public class ConverterOutputTest
    extends ConverterTest
    {
    private String expectedOutput;

    public ConverterOutputTest(
             String name,
             String input,
             String expectedOutput,
             ConversionMode mode)
        {
        super(name, input, mode);
        this.expectedOutput = trim(expectedOutput);
        }
    
    @Test
    public void testConversion()
        throws IOException
        {
        String output = convert();
        assert expectedOutput.equals(output)
            : "Mismatched conversion result for " + getTestName()
                + "\n" + showInvisibles(expectedOutput)
                + "\n" + showInvisibles(output);
        }
    }
