/*
 * (C) 2010-2012 ICM UW. All rights reserved.
 */
package pl.edu.icm.coansys.importers;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.icm.coansys.importers.DocumentProtos.Document;
import pl.edu.icm.coansys.importers.DocumentProtos.DocumentMetadata;
import pl.edu.icm.coansys.importers.DocumentProtos.Media;
import pl.edu.icm.synat.application.model.bwmeta.YContentEntry;
import pl.edu.icm.synat.application.model.bwmeta.YContentFile;
import pl.edu.icm.synat.application.model.bwmeta.YElement;
import pl.edu.icm.synat.application.model.bwmeta.YExportable;

/**
 *
 * @author Artur Czeczko a.czeczko@icm.edu.pl
 */
public class ZipDirToProtos implements Iterable<Document> {
    /*
     * The directory contains multiple zip files. Every zip file can contain
     * multiple xml files. Every xml file can contain multiple YExportable
     * objects. An iterator of this class walks through every zip file, then
     * every xml file and every YExportable object. Output type
     * DocumentMetadata.Builder is a class generated by protocol buffers
     * compiler.
     */

    private static final Logger logger = LoggerFactory.getLogger(ZipDirToProtos.class);
    //List of zip files to process and actual position in this list
    private File[] listZipFiles;
    private int zipIndex;
    //A zip archive we are processing
    private ZipArchive actualZipArchive = null;
    //Markers of actual position in archive
    private Iterator<String> xmlPathIterator = null;
    private Iterator<YExportable> yExportableIterator = null;
    //An object which will be returned by next call of iterators next() method
    private Document nextItem = null;

    public ZipDirToProtos(String zipDirPath) {
        File zipDir = new File(zipDirPath);
        if (zipDir.isDirectory()) {
            listZipFiles = zipDir.listFiles(new ZipFilter());
            zipIndex = 0;
            moveToNextItem();
        } else {
            logger.error(ZipDirToProtos.class.getName() + ": " + zipDirPath + " is not a directory");
        }
    }

    @Override
    public Iterator<Document> iterator() {
        return new Iterator() {

            @Override
            public boolean hasNext() {
                return nextItem != null;
            }

            @Override
            public Document next() {
                Document actualItem = nextItem;
                moveToNextItem();
                return actualItem;
            }

            @Override
            public void remove() {
                moveToNextItem();
            }
        };
    }

    private void moveToNextItem() {
        Document.Builder docBuilder = null;

        while (docBuilder == null) {
            while (yExportableIterator == null || !yExportableIterator.hasNext()) {
                while (xmlPathIterator == null || !xmlPathIterator.hasNext()) {
                    if (listZipFiles == null || zipIndex >= listZipFiles.length) {
                        nextItem = null;
                        return;
                    }
                    // here we have a new zip file
                    try {
                        actualZipArchive = new ZipArchive(listZipFiles[zipIndex].getPath());
                        xmlPathIterator = actualZipArchive.filter(".*xml").iterator();
                    } catch (IOException ex) {
                        logger.error(ex.toString());
                    }
                    zipIndex++;
                }
                // here we have a new xml path:
                String xmlPath = xmlPathIterator.next();
                try {
                    InputStream xmlIS = actualZipArchive.getFileAsInputStream(xmlPath);
                    yExportableIterator = MetadataPBParser.streamToYExportable(xmlIS, MetadataPBParser.MetadataType.BWMETA).iterator();
                } catch (IOException ex) {
                    logger.error(ex.toString());
                }
            }
            // here we have an yExportable:
            YExportable yExportable = yExportableIterator.next();

            if (yExportable instanceof YElement) {
                YElement yElement = (YElement) yExportable;

                DocumentMetadata docMetadata = MetadataPBParser.yelementToDocumentMetadata(yElement);
                
                if (docMetadata != null) {
                    docBuilder = Document.newBuilder();
                    docBuilder.setKey(docMetadata.getKey()); //Document and DocumentMetadata should have the same key?
                    docBuilder.setMetadata(docMetadata);

                    List<YContentEntry> contents = yElement.getContents();
                    for (YContentEntry content : contents) {
                        InputStream pdfIS = null;
                        //get a pdf path from yElement
                        if (content.isFile()) {

                            YContentFile yFile = (YContentFile) content;

                            //supported format: PDF
                            //Here you can add support of other formats (see also setMediaType() below)
                            if ("application/pdf".equals(yFile.getFormat())) {
                                for (String location : yFile.getLocations()) {
                                    //path to pdf in yFile contains prefix yadda.pack:/, check and remove it
                                    String prefix = "yadda.pack:/";
                                    if (location.startsWith(prefix)) {
                                        location = location.substring(prefix.length());
                                        //path to pdf in zip file contains zip filename, not included in yFile
                                        List<String> foundPaths = actualZipArchive.filter(".*" + location);
                                        //foundPaths should contain 1 item
                                        if (foundPaths.size() > 0) {
                                            try {
                                                pdfIS = actualZipArchive.getFileAsInputStream(foundPaths.get(0));
                                                // ... do something with pdfIS
                                                Media.Builder mediaBuilder = Media.newBuilder();
                                                mediaBuilder.setKey(nextItem.getKey()); //Media and Document should have the same key?
                                                mediaBuilder.setMediaType("PDF"); //??
                                                mediaBuilder.setContent(ByteString.copyFrom(IOUtils.toByteArray(pdfIS)));
                                                docBuilder.addMedia(mediaBuilder.build());
                                            } catch (IOException ex) {
                                                logger.error(ex.toString());
                                            }
                                        } else {
                                            logger.error("File path in BWmeta, but not in archive: " + location);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        nextItem = docBuilder.build();
    }

    private static class ZipFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return (name.endsWith(".zip"));
        }
    }
}
