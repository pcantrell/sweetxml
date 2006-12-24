package net.innig.sweetxml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileConverterEngine
    {
    private ConversionMode mode;

    public FileConverterEngine(ConversionMode mode)
        { this.mode = mode; }
    
    public void convert(File inFile)
        throws IOException
        {
        if(inFile.isDirectory())
            convertDir(inFile);
        else
            convertFile(inFile);
        }

    public void convertDir(File dir)
        throws IOException
        {
        checkExists(dir);
        if(!dir.isDirectory())
            throw new IllegalArgumentException(dir.getPath() + " is not a directory");
        
        for(File f : dir.listFiles())
            {
            if(f.isDirectory() && !f.getName().startsWith("."))
                convertDir(f);
            else if(f.getName().endsWith(mode.getSourceExtension()))
                convertFile(f);
            }
        }

    public void convertFile(File inFile)
        throws IOException
        {
        String outFileName = inFile.getName();
        if(outFileName.endsWith(mode.getSourceExtension()))
            outFileName = outFileName.substring(0, outFileName.length() - mode.getSourceExtension().length());
        outFileName += mode.getTargetExtension();
        File outFile = new File(inFile.getParentFile(), outFileName);
        
        convertFile(inFile, outFile);
        }

    public void convertFile(File inFile, File outFile)
        throws IOException
        {
        checkExists(inFile);
        
        System.out.println("Converting " + inFile + " to " + outFile);
        
        Converter converter = mode.createConverter();
        converter.setInput(new FileReader(inFile));
        
        FileWriter out = new FileWriter(outFile);
        out.write(converter.getResult());
        out.close();
        }

    private void checkExists(File f)
        throws FileNotFoundException
        {
        if(!f.exists())
            throw new FileNotFoundException(f.getPath() + " does not exist");
        }
    }
