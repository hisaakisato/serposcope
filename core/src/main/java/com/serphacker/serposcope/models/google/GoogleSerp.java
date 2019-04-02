/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.models.google;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.serphacker.serposcope.scraper.google.GoogleScrapLinkEntry;


public class GoogleSerp {
    
    private final static byte SERIAL_VERSION = 0;
    
    int runId;
    int googleSearchId;
    LocalDateTime runDay;
    List<GoogleSerpEntry> entries = new ArrayList<>();
    GoogleSearch search;

    public GoogleSerp(int runId, int googleSearchId, LocalDateTime runDay) {
        this.runId = runId;
        this.googleSearchId = googleSearchId;
        this.runDay = runDay;
    }
    
    public void addEntry(GoogleSerpEntry entry){
        entries.add(entry);
    }

    public int getRunId() {
        return runId;
    }
    public int getGoogleSearchId() {
        return googleSearchId;
    }
    
    public List<GoogleSerpEntry> getEntries() {
        return entries;
    }

    public LocalDateTime getRunDay() {
        return runDay;
    }

    public void setRunDay(LocalDateTime runDay) {
        this.runDay = runDay;
    }
    
    
    public GoogleSearch getSearch() {
		return search;
	}

	public void setSearch(GoogleSearch search) {
		this.search = search;
	}

	public void setSerializedEntries(byte[] data) throws IOException{
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        
        byte version = dis.readByte();
        if(version != SERIAL_VERSION){
            throw new UnsupportedOperationException("unsupported serialized version");
        }

        int entrySize = dis.readShort();
        entries = new ArrayList<>(entrySize);
        
        for (int i = 0; i < entrySize; i++) {
        	GoogleScrapLinkEntry linkEntry = new GoogleScrapLinkEntry(dis.readUTF());
            GoogleSerpEntry entry = new GoogleSerpEntry(linkEntry);
            byte mapSize = dis.readByte();
            if (mapSize < 0) {
            	// check version
            	int linkEntryVersion = mapSize * -1;
            	switch (linkEntryVersion) {
            	case 1:
            		linkEntry.setTitle(dis.readUTF());
            		String nonAmpUrl = dis.readUTF();
            		if (nonAmpUrl != null && !nonAmpUrl.isEmpty()) {
            			linkEntry.setNonAmpUrl(nonAmpUrl);
            		}
            		int rank = dis.readInt();
            		if (rank >= 0) {
            			linkEntry.setFeaturedRank(rank);
            		}
            	}
                mapSize = dis.readByte();
            }
            for (int j = 0; j < mapSize; j++) {
                short key = dis.readShort();
                short value = dis.readShort();
                entry.map.put(key, value);
            }
            entries.add(entry);
        }
    }
    
    public byte[] getSerializedEntries() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(SERIAL_VERSION);
        dos.writeShort(entries.size());
        for (GoogleSerpEntry entry : entries) {
            dos.writeUTF(entry.url);
            dos.writeByte(-1 * GoogleScrapLinkEntry.SERIAL_VERSION);
            dos.writeUTF(entry.title);
            dos.writeUTF(entry.nonAmpUrl == null ? "" : entry.nonAmpUrl);
            dos.writeInt(entry.featuredRank == null ? -1 : entry.featuredRank.intValue());
            dos.writeByte(entry.map.size());
            for (Map.Entry<Short, Short> mapEntry : entry.map.entrySet()) {
                dos.writeShort(mapEntry.getKey());
                dos.writeShort(mapEntry.getValue());
            }
        }
        
        baos.close();
        return baos.toByteArray();
    }
    
    
    

}
