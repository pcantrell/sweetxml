package net.innig.sweetxml;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.util.Arrays;

import org.testng.annotations.Test;

public class ParseExceptionTest
    extends ConverterTest
    {
    private SweetXmlMessage expectedMessage;
    private String expectedMessageArgs;
    private DocumentPosition expectedPosition;

    public ParseExceptionTest(
            String name,
            String input,
            SweetXmlMessage expectedMessage,
            String expectedMessageArgs,
            DocumentPosition expectedPosition,
            ConversionMode mode)
        {
        super(name, input, mode);
        this.expectedMessage = expectedMessage;
        this.expectedMessageArgs = expectedMessageArgs;
        this.expectedPosition = expectedPosition;
        }
    
    @Test
    public void testParseExcecption()
        throws IOException
        {
        try {
            String output = convert();
            fail("Expected parse exception, but got output:\n" + showInvisibles(output));
            }
        catch(SweetXmlParseException pe)
            {
            assertEquals(
                
                Arrays.asList(
                    expectedPosition,
                    expectedMessage,
                    expectedMessageArgs),
                    
                Arrays.asList(
                    pe.getPosition(),
                    pe.getMessageId(),
                    Arrays.toString(pe.getMessageArgs())));
            }
        }
    }
