package net.innig.sweetxml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A utility class for converting files, either singly or en masse.
 */
public class FileConverterEngine
    {
    private boolean overwrite, quiet;

    /**
     * Creates a new converter.
     * @param overwrite Should the converter overwrite existing output files?
     * @param quiet     Should the converter suppress non-error output?
     */
    public FileConverterEngine(boolean overwrite, boolean quiet)
        {
        this.overwrite = overwrite;
        this.quiet = quiet;
        }
    
    /**
     * Converts the given file or directory.
     * 
     * @see #convertFile(File, ConversionMode)
     * @see #convertDir(File, ConversionMode)
     */
    public void convert(File inFile, ConversionMode mode)
        throws IOException
        {
        if(inFile.isDirectory())
            convertDir(inFile, mode);
        else
            convertFile(inFile, mode);
        }
    
    /**
     * Traverses the given directory and its descendants, converting files
     * in place as it finds them.
     */
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
    
    /**
     * Converts the given file in place, i@.e@. placing the output file in the same directory as
     * the input file. The output file will have an extension appropriate to the given mode, in
     * the manner of {@link #outputFileFor(File, ConversionMode)}.
     */
    public void convertFile(File inFile, ConversionMode mode)
        throws IOException
        {
        convertFile(inFile, outputFileFor(inFile, mode), mode);
        }
    
    /**
     * Determines the appropriate name the output of the given file being converted in the
     * given mode. Does not create the file.
     */
    public File outputFileFor(File inFile, ConversionMode mode)
        {
        String outFileName = inFile.getName();
        if(outFileName.endsWith(mode.getSourceExtension()))
            outFileName = outFileName.substring(0, outFileName.length() - mode.getSourceExtension().length());
        outFileName += mode.getTargetExtension();
        return new File(inFile.getParentFile(), outFileName);
        }

    /**
     * Converts a file, creating the given output file if necessary.
     */
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
            {
            System.err.println("Converting " + inFile);
            System.err.println("        to " + outFile);
            }
        
        Converter converter = mode.createConverter();
        converter.setInput(new FileReader(inFile), inFile.getPath());
        
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
