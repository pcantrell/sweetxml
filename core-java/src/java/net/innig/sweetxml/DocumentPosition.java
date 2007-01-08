package net.innig.sweetxml;

/**
 * Represents a position within a text file. Used for locating errors.
 */
public final class DocumentPosition
    {
    private final String sourceName;
    private final int line, column;
    
    public DocumentPosition(String sourceName, int line, int column)
        {
        this.sourceName = sourceName;
        this.line = line;
        this.column = column;
        }
    
    public int getColumn()
        { return column; }
    
    public int getLine()
        { return line; }
    
    public String getSourceName()
        { return sourceName; }
    
    /**
     * Returns a user-readable summary of this position.
     */
    @Override
    public String toString()
        { return sourceName + ':' + line + ':' + column; }
    }
