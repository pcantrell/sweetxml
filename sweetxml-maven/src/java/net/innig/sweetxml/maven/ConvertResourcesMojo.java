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
 * Converts SweetXML files in the output directory to XML files.
 * <p>
 * Portions based on ResourceMojo.java from Maven, written by Michal Maczka, Jason van Zyl and Andreas Hoheneder.
 *
 * @author Paul Cantrell
 * @goal convert-resources
 * @phase process-resources
 */
public class ConvertResourcesMojo
    extends AbstractMojo
    {
    /**
     * The character encoding scheme to be applied.
     * 
     * @parameter
     */
    private String encoding;

    /**
     * The output directory into which to copy the resources.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private String outputDirectory;
    
    /**
     * The conversion mode. Maybe be "s2x" (the default) or "x2s".
     *
     * @parameter
     */
    private String mode;

    /**
     * The list of additional key-value pairs aside from that of the System, 
     * and that of the project, which would be used for the filtering. 
     * 
     * @parameter expression="${project.build.filters}"
     */
    private List filters;

    public void execute()
        throws MojoExecutionException
        {
        if(mode == null)
            mode = "s2x";
        ConversionMode modeParsed;
        try { modeParsed = ConversionMode.valueOf(mode.toUpperCase()); }
        catch(IllegalArgumentException iae)
            { throw new MojoExecutionException("No such mode \"" + mode + "\"; expected \"x2s\" or \"s2x\""); }
        
        File outputDir = new File(outputDirectory);
        if(!outputDir.exists())
            throw new MojoExecutionException("Output directory does not exist: " + outputDir);

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(outputDir);
        scanner.setIncludes(new String[] { "**/**" + modeParsed.getSourceExtension() });
        scanner.addDefaultExcludes();
        scanner.scan();
        
        FileConverterEngine convert = new FileConverterEngine(modeParsed);
        for(String inFile : scanner.getIncludedFiles())
            try {
                convert.convertFile(
                    new File(outputDir, inFile));
                }
            catch(IOException ioe)
                {
                throw new MojoExecutionException(
                    "Unable to convert " + inFile
                        + " from " + modeParsed.getSourceExtension()
                        + " to " + modeParsed.getTargetExtension(),
                    ioe);
                }
        }
    }
