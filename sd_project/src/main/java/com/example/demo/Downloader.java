//Downloader -------------------
package com.example.demo;

import java.net.MulticastSocket;
import java.rmi.Naming;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * Represents a Downloader thread responsible for getting urls
 * from the Queue (by RMI), searching those web pages (with jsoup),
 * extracting information, and sending it to the Barrels via multicast.
 */
public class Downloader extends Thread {
    
    private String MULTICAST_ADDRESS;
    private int PORT;


    //Downloader
    /**
     * Constructs a new Downloader thread.
     * Gets the multicast address and port
     * from the properties file.
     */
    public Downloader() {
        super("Server " + (long) (Math.random() * 1000));

        //Get Multicast infos from properties file
        try (BufferedReader br = new BufferedReader(new FileReader("properties.txt"))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                //Get Multicast Address
                if (parts[0].trim().equals("Multicast Address")) { MULTICAST_ADDRESS = parts[1].trim(); }
                //Get Multicast Port
                else if (parts[0].trim().equals("Multicast Port")) { PORT = Integer.parseInt(parts[1].trim()); }
            }

            //Debug
            //System.out.println("Mult address: " + MULTICAST_ADDRESS + " / Mult port: " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
            MULTICAST_ADDRESS = "224.3.2.1";
            PORT = 4321;
        }
    }


    //Multicast server
    /**
     * Runs the multicast server to continuously download
     * web pages, extract information, and send it to the Barrels.
     */
    public void run() {
        
        MulticastSocket socket = null;
        System.out.println(this.getName() + " running...");
        try {
            //Create socket without binding it (only for sending)
            socket = new MulticastSocket();
            while (true) {

                //Get RMI data from properties file
                String RMI_DATA= "localhost";
                
                try (BufferedReader br = new BufferedReader(new FileReader("properties.txt"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts[0].trim().equals("RMI Address")) { RMI_DATA = parts[1].trim(); }
                        if (parts[0].trim().equals("RMI Port")) { RMI_DATA = RMI_DATA + ":" + Integer.parseInt(parts[1].trim()); }
                    }
                } catch (IOException e) { e.printStackTrace(); }

                try {
                    //Get url from queue
                    IQueue urlQueue = (IQueue) Naming.lookup("rmi://" + RMI_DATA + "/RMIQueue");
                    String url= urlQueue.popQueue();

                    //Search url's web page
                    JSONObject json = searchWebPage(url);

                    //Check if url page was available
                    if("Not available".equals(json.getString("url"))){
                        continue;
                    }
                    
                    //Send results to Barrels by chunks
                    //Chunk size
                    int chunkSize = 50000;
                    //Number chunks needed
                    int numChunks = (int) Math.ceil((double) ((double) json.getString("url").length()
                    + (double) json.getString("title").length()
                    + (double) json.getString("text").length()
                    + (double) json.getJSONArray("links").length()) / chunkSize);
                    System.out.println(numChunks);

                    //Send each chunk
                    for (int i = 0; i < numChunks; i++) {
                        //Calculate start and end index
                        int startIndex = i * chunkSize;
                        int endIndex = Math.min(startIndex + chunkSize, json.getString("text").length());
        
                        //Create a new JSON object for this chunk
                        JSONObject chunkJson = new JSONObject();
                        chunkJson.put("url", json.getString("url"));
                        chunkJson.put("title", json.getString("title"));
                        chunkJson.put("text", json.getString("text").substring(startIndex, endIndex));
                        chunkJson.put("links", json.getJSONArray("links"));
        
                        //Convert the JSON object to string and then to byte array
                        String jsonData = chunkJson.toString();
                        byte[] buffer = jsonData.getBytes();
        
                        //Send the packet by multicast
                        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                        socket.send(packet);
                    }

                    //Put links found on queue
                    JSONArray linksArray = json.getJSONArray("links");
                    for (int i = 0; i < linksArray.length(); i++) {
                        urlQueue.putQueue(linksArray.getString(i));
                    }

                } catch (Exception e) {
                    System.err.println("Error in Queue: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
    

    //Search web page using Jsoup
    /**
     * Searches a web page using Jsoup library and extracts
     * the informations: title, text, and links.
     * @param url The URL of the web page to be searched.
     * @return A JSONObject containing the extracted information.
     */
    private JSONObject searchWebPage(String url) {

        System.out.println("Searching url: " + url);
        JSONObject json = new JSONObject();
        
        //Get info's
        try {

            //Url - - -
            json.put("url", url);

            //Title - - -
            Document doc = Jsoup.connect(url).get();
            json.put("title", doc.title());

            //Text - - - 
            Elements paragraphs = doc.select("p");
            StringBuilder textBuilder = new StringBuilder();

            for (Element paragraph : paragraphs) {
                textBuilder.append(paragraph.text()).append("\n");
            }

            String text = textBuilder.toString();
            json.put("text", text);

            //Links - - -
            Elements links = doc.select("a[href]");
            JSONArray linksArray = new JSONArray();

            for (Element link : links) {
                String absoluteUrl = link.attr("abs:href");
                linksArray.put(absoluteUrl);
            }

            json.put("links", linksArray);

        } catch (IOException e) {
            e.printStackTrace();
            json.put("url", "Not available");

        }
        
        return json;
    }


    //Main
    /**
     * Main method to initiate the Downloader thread.
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        
        try {
            Downloader downloader = new Downloader();
            System.out.println("initiating downloader " + downloader.getName());
            downloader.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}