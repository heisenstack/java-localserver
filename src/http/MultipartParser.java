package src.http;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MultipartParser {
    
    public static class Part {
        private String name;
        private String filename;
        private String contentType;
        private byte[] data;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        
        public String getDataAsString() {
            return new String(data, StandardCharsets.UTF_8);
        }
        
        public boolean isFile() {
            return filename != null && !filename.isEmpty();
        }
    }
    

    public static List<Part> parse(byte[] body, String boundary) throws IOException {
        List<Part> parts = new ArrayList<>();
        
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] endBoundaryBytes = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);
        
        int pos = 0;
        
        pos = findBoundary(body, boundaryBytes, pos);
        if (pos == -1) return parts;
        pos += boundaryBytes.length + 2; 
        
        while (pos < body.length) {
            int nextBoundary = findBoundary(body, boundaryBytes, pos);
            int endBoundary = findBoundary(body, endBoundaryBytes, pos);
            
            if (nextBoundary == -1 && endBoundary == -1) break;
            
            int partEnd = (endBoundary != -1 && (nextBoundary == -1 || endBoundary < nextBoundary)) 
                          ? endBoundary : nextBoundary;
            
            Part part = parsePart(body, pos, partEnd);
            if (part != null) {
                parts.add(part);
            }
            
            if (endBoundary != -1 && endBoundary == partEnd) {
                break; 
            }
            
            pos = partEnd + boundaryBytes.length + 2; 
        }
        
        return parts;
    }
    

    private static Part parsePart(byte[] body, int start, int end) throws IOException {
        int headerEnd = findSequence(body, "\r\n\r\n".getBytes(), start, end);
        if (headerEnd == -1) return null;
        
        String headers = new String(body, start, headerEnd - start, StandardCharsets.UTF_8);
        
        Part part = new Part();
        parsePartHeaders(headers, part);
        
        int dataStart = headerEnd + 4;
        int dataEnd = end - 2; 
        
        if (dataEnd > dataStart) {
            byte[] data = new byte[dataEnd - dataStart];
            System.arraycopy(body, dataStart, data, 0, data.length);
            part.setData(data);
        } else {
            part.setData(new byte[0]);
        }
        
        return part;
    }
    

    private static void parsePartHeaders(String headers, Part part) {
        String[] lines = headers.split("\r\n");
        
        for (String line : lines) {
            if (line.toLowerCase().startsWith("content-disposition:")) {
                parseContentDisposition(line, part);
            } else if (line.toLowerCase().startsWith("content-type:")) {
                String contentType = line.substring(13).trim();
                part.setContentType(contentType);
            }
        }
    }
    
    private static void parseContentDisposition(String line, Part part) {
        String[] tokens = line.split(";");
        
        for (String token : tokens) {
            token = token.trim();
            
            if (token.startsWith("name=")) {
                String name = token.substring(5).replaceAll("\"", "");
                part.setName(name);
            } else if (token.startsWith("filename=")) {
                String filename = token.substring(9).replaceAll("\"", "");
                part.setFilename(filename);
            }
        }
    }
    

    private static int findBoundary(byte[] data, byte[] boundary, int start) {
        return findSequence(data, boundary, start, data.length);
    }
    

    private static int findSequence(byte[] data, byte[] sequence, int start, int end) {
        if (sequence.length == 0) return -1;
        
        outer:
        for (int i = start; i <= end - sequence.length; i++) {
            for (int j = 0; j < sequence.length; j++) {
                if (data[i + j] != sequence[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}