package net.innig.sweetxml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Factory;

public class ConverterTestFactory
    {
    @Factory
    public Object[] createConverterTests()
        throws IOException
        {
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
                
                StringBuilder
                    input = readChunk(testIn),
                    expectedOutput = readChunk(testIn);
                
                tests.add(
                    new ConverterTest(
                        fileName + ": " + testName,
                        input.toString(),
                        expectedOutput.toString(),
                        mode));
                }
            testIn.close();
            }
        return tests.toArray();
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
