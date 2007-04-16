package net.innig.sweetxml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Factory;

public class ConverterTestFactory
    {
    private static final Pattern parseErrorPat = Pattern.compile("\\s*!\\s*([A-Z0-9_]+)\\s+(\\d+):(\\d+)\\s+(\\[.*\\])\\s*");
    
    @Factory
    public Object[] createConverterTests()
        throws IOException
        {
        Set<SweetXmlMessage> unusedMessages = EnumSet.allOf(SweetXmlMessage.class);
        unusedMessages.remove(SweetXmlMessage.INDENT_SPACES);
        unusedMessages.remove(SweetXmlMessage.INDENT_TABS);
        unusedMessages.remove(SweetXmlMessage.INDENT_UNICODE);
        
        List<ConverterTest> tests = new ArrayList<ConverterTest>();
        for(File testFile : new File("test/res/converter-tests/").listFiles())
            {
            String fileName = testFile.getName();
            if(!fileName.endsWith(".test"))
                continue;
            fileName = fileName.substring(0, fileName.length() - ".test".length());
            ConversionMode mode = ConversionMode.valueOf(
                fileName.substring(0, fileName.indexOf('-')).toUpperCase());
            
            BufferedReader testIn = new BufferedReader(
                new InputStreamReader(new FileInputStream(testFile), "utf-8"));
            
            while(true)
                {
                String testName = readChunk(testIn).toString().trim();
                if(testName.length() == 0)
                    break;
                
                String
                    name     = fileName + ": " + testName,
                    input    = unescape(readChunk(testIn).toString()),
                    expected = unescape(readChunk(testIn).toString());
                
                if(expected.trim().startsWith("!"))
                    {
                    Matcher m = parseErrorPat.matcher(expected);
                    if(!m.matches())
                        throw new IOException("Test \"" + name + "\" has a misformatted expected error output");
                    SweetXmlMessage message = SweetXmlMessage.valueOf(m.group(1));
                    unusedMessages.remove(message);
                    tests.add(
                        new ParseExceptionTest(
                            name,
                            input,
                            message,
                            m.group(4),
                            new DocumentPosition(
                                name,
                                Integer.parseInt(m.group(2)),
                                Integer.parseInt(m.group(3))),
                            mode));
                    }
                else
                    tests.add(
                        new ConverterOutputTest(name, input, expected, mode));
                }
            testIn.close();
            }
        
        if(!unusedMessages.isEmpty())
            {
            System.err.println("WARNING: The following parse exception messages are not tested:");
                for(SweetXmlMessage msg : unusedMessages)
                    System.err.println("    " + msg);
            }
        
        return tests.toArray();
        }
    
    private String unescape(String text)
        {
        return text
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\n", "\n");
        }

    private StringBuilder readChunk(BufferedReader in)
        throws IOException
        {
        StringBuilder chunk = new StringBuilder();
        while(true)
            {
            String line = in.readLine();
            if(line == null || line.startsWith("———"))
                return chunk;
            chunk.append(line).append('\n'); //! work out this separator business...
            }
        }
    }
