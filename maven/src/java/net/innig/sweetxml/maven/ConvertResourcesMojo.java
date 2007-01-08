package net.innig.sweetxml.maven;

import net.innig.sweetxml.ConversionMode;
import net.innig.sweetxml.FileConverterEngine;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Converts SweetXML files in the output directory to (or from) XML files.
 *
 * @author Paul Cantrell.
 *      Portions based on ResourceMojo.java from Maven, written
 *      by Michal Maczka, Jason van Zyl and Andreas Hoheneder.
 * @goal  convert-resources
 * @phase process-resources
 */
public class ConvertResourcesMojo
    extends AbstractMojo
    {
    /**
     * The directory in which there are resources to be converted.
     * By default, this is the same as the outputDirectory.
     *
     * @parameter
     */
    private String inputDirectory;
    
    /**
     * The directory in which to place the converted resources.
     * By default, this is the project's outputDirectory.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;
    
    /**
     * The conversion mode. May be "s2x" (the default) or "x2s".
     *
     * @parameter
     */
    private String mode;
    
    /**
     * If true, the plugin will not print information about which files are being converted.
     * True by default.
     *
     * @parameter
     */
    private boolean quiet = true;
    
    /**
     * If true, the plugin will overwrite existing output files. True by default.
     *
     * @parameter
     */
    private boolean overwrite = true;
    
    /**
     * If true, the plugin will delete files from the output directory after converting them.
     * False by default.
     *
     * @parameter
     */
    private boolean deleteSources;

    public void execute()
        throws MojoExecutionException
        {
        if(mode == null)
            mode = "s2x";
        ConversionMode modeParsed;
        try { modeParsed = ConversionMode.valueOf(mode.toUpperCase()); }
        catch(IllegalArgumentException iae)
            { throw new MojoExecutionException("No such mode \"" + mode + "\"; expected \"x2s\" or \"s2x\""); }
        
        if(inputDirectory == null)
            inputDirectory = outputDirectory;
        File inputDir = new File(inputDirectory);
        File outputDir = new File(outputDirectory);
        if(!inputDir.exists())
            throw new MojoExecutionException("Input directory does not exist: " + inputDir);
        if(!outputDir.exists())
            throw new MojoExecutionException("Output directory does not exist: " + outputDir);

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(inputDir);
        scanner.setIncludes(new String[] { "**/**" + modeParsed.getSourceExtension() });
        scanner.addDefaultExcludes();
        scanner.scan();
        
        FileConverterEngine convert = new FileConverterEngine(overwrite, quiet);
        for(String inFileName : scanner.getIncludedFiles())
            try {
                File inFile = new File(outputDir, inFileName);
                convert.convertFile(inFile, modeParsed);
                if(deleteSources)
                    inFile.delete();
                }
            catch(IOException ioe)
                {
                throw new MojoExecutionException(
                    "Unable to convert " + inFileName
                        + " from " + modeParsed.getSourceExtension()
                        + " to " + modeParsed.getTargetExtension(),
                    ioe);
                }
        }
    }
