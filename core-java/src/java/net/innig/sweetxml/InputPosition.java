package net.innig.sweetxml;

public final class InputPosition
    {
    private final String sourceName;
    private final int line, column;
    
    public InputPosition(String sourceName, int line, int column)
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
    
    @Override
    public String toString()
        { return sourceName + ':' + line + ':' + column; }
    }
