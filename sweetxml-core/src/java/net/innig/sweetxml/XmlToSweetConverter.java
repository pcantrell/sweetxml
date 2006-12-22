package net.innig.sweetxml;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlToSweetConverter
    extends Converter
    {
    private static final Pattern
        quotingRequiredPat = Pattern.compile("[\"'#:,\r\n\\s]"),
        initialWhitespacePat = Pattern.compile("(\\s*)(.*)", Pattern.DOTALL),
        lineBreakPat = Pattern.compile("\r\n|\r|\n");
    private DocumentBuilderFactory parserFactory;
    
    public XmlToSweetConverter()
        {
        parserFactory = DocumentBuilderFactory.newInstance();
        parserFactory.setValidating(false);
        parserFactory.setNamespaceAware(false);
        }

    protected String convert()
        throws IOException
        {
        try {
            return new DomConverter(
                    parserFactory.newDocumentBuilder().parse(
                        new InputSource(getInput())))
                .go().toString();
            }
        catch(SAXException e)
            { throw wrapException(e); }
        catch(ParserConfigurationException e)
            { throw wrapException(e); }
        }
    
    private IOException wrapException(Exception e)
        {
        IOException ioe = new IOException("Unable to parse XML");
        ioe.initCause(e);
        return ioe;
        }

    private class DomConverter
        {
        private Document document;
        private StringBuilder out;
        private int indent;
        private int lineBreaksPending;
        private boolean insideTag, onTagLine;
        private String newline = System.getProperty("line.separator");

        public DomConverter(Document document)
            { this.document = document; }

        public CharSequence go()
            {
            out = new StringBuilder();
            handleElement(document.getDocumentElement());
            return out;
            }

        private void handleElement(Element elem)
            {
            flushLineBreaks();
            if(onTagLine)
                {
                out.append(newline);
                printIndent();
                }
            
            insideTag = true;
            onTagLine = true;
            out.append(convertName(elem.getTagName()));
            handleAttributes(elem.getAttributes());
            indent++;
            
            NodeList children = elem.getChildNodes();
            for(int n = 0; n < children.getLength(); n++)
                handleNode(children.item(n));
            
            indent--;
            if(lineBreaksPending > 0)
                lineBreaksPending--;
            }

        private void handleAttributes(NamedNodeMap attrs)
            {
            for(int n = 0; n < attrs.getLength(); n++)
                {
                Attr attr = (Attr) attrs.item(n);
                out.append(' ').append(convertName(attr.getName())).append('=');
                printQuoted(attr.getValue(), false);
                }
            }

        private void handleNode(Node node)
            {
            if(node instanceof Element)
                handleElement((Element) node);
            else if(node instanceof Text)
                handleText((Text) node);
            else if(node instanceof Comment)
                handleComment((Comment) node);
            else
                System.out.println("WARNING: unknown node type: " + node.getClass() + ": " + node);
            }
        
        private void handleText(Text textNode)
            {
            Matcher space = initialWhitespacePat.matcher(textNode.getTextContent());
            space.find();
            
            for(Matcher m = lineBreakPat.matcher(space.group(1)); m.find(); )
                lineBreaksPending++;
            
            if(space.group(2).length() > 0)
                {
                flushLineBreaks();
                if(insideTag)
                    out.append(": ");
                insideTag = false;
                printQuoted(space.group(2), !onTagLine);
                }
            }
        
        private void handleComment(Comment comment)
            {
            flushLineBreaks();
            out.append("#").append(comment.getTextContent().replace("\n", "|"));
            }

        private void printIndent()
            {
            for(int n = indent; n > 0; n--)
                out.append("    ");
            }
        
        private void printQuoted(String s, boolean forceQuotes)
            {
            if(!forceQuotes && !quotingRequiredPat.matcher(s).find())
                out.append(s);
            else if(s.contains("'") || !s.contains("\""))
                out.append("\"").append(s.replace("\"", "&quot;")).append("\"");
            else
                out.append("'").append(s).append("'");
            }
        
        private String convertName(String qName)
            { return qName.replace(":", "/"); }
        
        private void flushLineBreaks()
            {
            while(lineBreaksPending-- > 0)
                {
                insideTag = onTagLine = false;
                out.append(newline);
                printIndent();
                }
            lineBreaksPending = 0;
            }
        }
    }