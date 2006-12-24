package net.innig.sweetxml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileConverterEngine
    {
    private boolean overwrite, quiet;

    public FileConverterEngine(boolean overwrite, boolean quiet)
        {
        this.overwrite = overwrite;
        this.quiet = quiet;
        }

    public void convert(File inFile, ConversionMode mode)
        throws IOException
        {
        if(inFile.isDirectory())
            convertDir(inFile, mode);
        else
            convertFile(inFile, mode);
        }

    public void convertDir(File dir, ConversionMode mode)
        throws IOException
        {
        checkExists(dir);
        if(!dir.isDirectory())
            throw new IllegalArgumentException(dir.getPath() + " is not a directory");
        
        for(File f : dir.listFiles())
            {
            if(f.isDirectory() && !f.getName().startsWith("."))
                convertDir(f, mode);
            else if(f.getName().endsWith(mode.getSourceExtension()))
                convertFile(f, mode);
            }
        }

    public void convertFile(File inFile, ConversionMode mode)
        throws IOException
        {
        String outFileName = inFile.getName();
        if(outFileName.endsWith(mode.getSourceExtension()))
            outFileName = outFileName.substring(0, outFileName.length() - mode.getSourceExtension().length());
        outFileName += mode.getTargetExtension();
        File outFile = new File(inFile.getParentFile(), outFileName);
        
        convertFile(inFile, outFile, mode);
        }

    public void convertFile(File inFile, File outFile, ConversionMode mode)
        throws IOException
        {
        checkExists(inFile);
        if(outFile.exists() && !overwrite)
            {
            if(!quiet)
                System.err.println("Warning: " + outFile + " already exists; skipping");
            return;
            }
        
        if(!quiet)
            System.err.println("Converting " + inFile + " to " + outFile);
        
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
