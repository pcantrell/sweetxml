package net.innig.sweetxml.ant;

import net.innig.sweetxml.ConversionMode;
import net.innig.sweetxml.FileConverterEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class SweetXmlTask
    extends Task
    {
    private ConversionMode mode = ConversionMode.S2X;
    private List<FileSet> inputFilesSets = new ArrayList<FileSet>();
    private File outputDir;
    private boolean quiet;
    private boolean overwrite = true;
    private boolean deleteSources;
    
    @Override
    public void execute()
        throws BuildException
        {
        if(inputFilesSets.isEmpty())
            throw new BuildException("The SweetXML task requires a nested inputFileSet element");
        
        FileConverterEngine converter = new FileConverterEngine(overwrite, quiet);
        for(FileSet inputFiles : inputFilesSets)
            {
            DirectoryScanner inScanner = inputFiles.getDirectoryScanner(getProject());
            File inputDir = inScanner.getBasedir();
            if(outputDir == null)
                outputDir = inputDir;
            for(String inFilePath : inScanner.getIncludedFiles())
                {
                File inFile = new File(inputDir, inFilePath);
                File outFile = new File(outputDir, inFilePath);
                outFile = converter.adjustFileExtentsion(outFile, mode);
                
                try {
                    if(!outFile.getParentFile().exists())
                        outFile.getParentFile().mkdirs();
                    
                    converter.convertFile(inFile, outFile, mode);
                    }
                catch(Exception e)
                    {
                    String sep = System.getProperty("line.separator");
                    throw new BuildException(
                        "Unable to perform SweetXML conversion" + sep
                        + "  from: " + inFile + sep
                        + "    to: " + outFile + sep
                        + "Reason: " + e, e);
                    }
                
                if(deleteSources)
                    inFile.delete();
                }
            }
        }
    
    public void setMode(String mode)
        throws BuildException
        {
        try { this.mode = ConversionMode.valueOf(mode.toUpperCase()); }
        catch(IllegalArgumentException iae)
            { throw new BuildException("No such mode \"" + mode + "\"; expected \"x2s\" or \"s2x\""); }
        }

    public void addConfiguredInputFileSet(FileSet inputFiles)
        throws IOException
        { inputFilesSets.add(inputFiles); }

    public void setOutputDir(File outputDir)
        { this.outputDir = outputDir; }

    public void setDeleteSources(boolean deleteSources)
        { this.deleteSources = deleteSources; }

    public void setOverwrite(boolean overwrite)
        { this.overwrite = overwrite; }

    public void setQuiet(boolean quiet)
        { this.quiet = quiet; }
    }
