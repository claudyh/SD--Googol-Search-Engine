//Index Barrel -------------------
package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * Barrel object that implements the IBarrel interface.
 * Barrels receive messages from the Downloaders (by multicast),
 * and store that information (urls, words and links) on inverted
 * indexes and files. The barrels also use a RMI Barrel connection
 * to inform the Gateway of their existance. And when they are restored
 * by the Gateway, they get their previous information from the files.
 * Barrel has also public methods to search words and links.
 */
public class Barrel extends UnicastRemoteObject implements IBarrel {

    private String MULTICAST_ADDRESS;
    private int PORT;

    //Inverted indexes
    private HashMap<String, HashSet<Integer>> wordsIndex;
    private HashMap<Integer, HashSet<String>> urlsIndex;
    private HashMap<String, HashSet<Integer>> linksIndex;


    //Barrel
    /**
     * Constructs a new Barrel object.
     * Initializes the inverted indexes.
     * @throws RemoteException if there is a problem with remote invocation.
     */
    public Barrel() throws RemoteException {
        super();
        this.wordsIndex = new HashMap<>();
        this.urlsIndex = new HashMap<>();
        this.linksIndex = new HashMap<>();

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
        } catch (IOException e) {
            e.printStackTrace();
            MULTICAST_ADDRESS = "224.3.2.1";
            PORT = 4321;
        }
    }


    //Multicast client
    /**
     * Runs the multicast client for receiving data packets.
     * @param currentBarrel The current Barrel object.
     * @param idBarrel The ID of the Barrel.
     * @param b The IRMIBarrel that connects to the Gateway.
     */    
    public void run(Barrel currentBarrel, int idBarrel, IRMIBarrel b) {

        MulticastSocket socket = null;
        try {
            //Create socket and bind it
            socket = new MulticastSocket(PORT);
            InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(new InetSocketAddress(mcastaddr, 0), NetworkInterface.getByIndex(0));

            while (true) {

                //Get data (by multicast)
                byte[] buffer = new byte[64000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String jsonData = new String(packet.getData(), 0, packet.getLength());
                JSONObject json = new JSONObject(jsonData);
                System.out.println("Received packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " with message:");


                //Check if url already exists
                int ID = -1;
                String ptext= "";
                Boolean newID= true;

                //If yes, get ID
                for (Integer id : urlsIndex.keySet()) {
                    HashSet<String> urlSet = urlsIndex.get(id);        
                    for (String str : urlSet) {
                        if(!str.startsWith("http") && !str.startsWith("Title: ")){
                            ptext= str;
                        }
                        if (str.equals(json.getString("url"))) {
                            //Already exists
                            ID= id;
                            newID= false;
                        }
                    }
                    if(!newID){ break;}
                }

                //If not, new ID
                if(newID){
                    //Define unique ID if url doenst have one
                    UUID uuid = UUID.randomUUID();
                    long mostSignificantBits = uuid.getMostSignificantBits();
                    ID = (int) (mostSignificantBits & Integer.MAX_VALUE);
                }


                //Add url to urlsIndex and linksIndex
                AddURL(ID, json, newID, ptext);
                System.out.println("Added url\n");


                //Add words to wordsIndex
                if (json.has("title")) { //Title words
                    AddWords(json.getString("title"), ID);
                }

                if (json.has("text")) { //Text words
                    AddWords(json.getString("text"), ID);
                }

                System.out.println("Added words:\n");

                //Save changes to file
                currentBarrel.SaveFile(idBarrel);
                //Inform gateway of new message
                b.BarrelNewMessage(idBarrel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }


    //Add url infos
    /**
     * Add URL information to the indexes.
     * @param ID The unique ID of the URL.
     * @param json The JSON object containing URL information.
     */
    public void AddURL(int ID, JSONObject json, Boolean newID, String previousText) {
        
        //Add url to urlsIndex - - -
        HashSet<String> infos = new HashSet<>();
                
        infos.add(json.getString("url"));
        if (json.has("title")) { infos.add("Title: " + json.getString("title")); } else { infos.add("Title: "); }

        if(!newID){
            //Add text to previous text
            if (json.has("text") && !previousText.equals(json.getString("text"))) {
                infos.add(previousText + " " + json.getString("text"));
            }
        } else{
            //Add text normally
            if (json.has("text")) { infos.add(json.getString("text")); } else { infos.add(""); }
        }

        urlsIndex.put(ID, infos);


        //Add links to linksIndex - - -
        if (json.has("links")) { 
            JSONArray linksArray = json.getJSONArray("links");
            
            for (int i = 0; i < linksArray.length(); i++) {
                String link = linksArray.getString(i);

                //if link already exists on index
                if(linksIndex.containsKey(link)){
                        //if doesn't have the ID
                        HashSet<Integer> linkSet = linksIndex.get(link);
                        if (!linkSet.contains(ID)) {
                            //Add ID
                            linksIndex.get(link).add(ID);
                        }
                } else{
                    //if link doesnt exist on index
                    HashSet<Integer> ids = new HashSet<>();
                    ids.add(ID);
                    linksIndex.put(link, ids);
                }
            }
        }
    }


    //Add words to inverted index
    /**
     * Add words to the inverted index.
     * @param text The text containing words to be added.
     * @param ID The ID of the URL associated with the words.
     */
    private void AddWords(String text, int ID) {

        StringTokenizer tokenizer = new StringTokenizer(text, " \t\n\r\f,.:;?![]'\"");

        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken().toLowerCase();
            
            //Check if word exists in wordsIndex
            if (wordsIndex.containsKey(word)) {
                //if doesn't have the ID
                HashSet<Integer> wordsSet = wordsIndex.get(word);
                if (!wordsSet.contains(ID)) {
                    //Add ID (url)
                    wordsIndex.get(word).add(ID);
                }
            } else {
                //Create a new word
                HashSet<Integer> idSet = new HashSet<>();
                idSet.add(ID);
                wordsIndex.put(word, idSet);
            }
        }
    }
    

    //Find phrase of searched words (aux function)
    /**
     * Find a phrase containing searched words within a given text.
     * Crop that phrase with a limit of 50 characters to the front
     * and back of the key word.
     * @param text The text to search within.
     * @param keyword The keyword to search for.
     * @return A string containing the phrase.
     */
    private String findPhrase(String text, String keyword) {

        //Check word position on text
        int index = text.toLowerCase().indexOf(keyword.toLowerCase());

        //If word doesnt exist
        if (index == -1) {
            return text.length() <= 50 ? text : text.substring(0, 50) + "...";
        }

        //If word exists
        int startIndex = Math.max(index - 50, 0);
        int endIndex = Math.min(index + keyword.length() + 50, text.length());
        StringBuilder phrase = new StringBuilder();

        if (startIndex > 0) { phrase.append("..."); }
        phrase.append(text.substring(startIndex, endIndex));
        if (endIndex < text.length()) { phrase.append("..."); }

        return phrase.toString();
    }


    //Search words
    /**
     * Search for words within the indexed URLs, and get the urls,
     * their titles and the part of text where the words appear.
     * Order the results by number of links that lead to the url,
     * and prioritize urls with the key words on the title.
     * @param words The words to search for.
     * @return A list of string arrays containing URL, title, cropped text, and order number.
     */
    @Override
    public ArrayList<String[]> SearchWords(String words){
        
        StringTokenizer tokenizer = new StringTokenizer(words, " \t\n\r\f,.:;?![]'\"");
        String nsWords = words.replaceAll("\\s", ""); //Words without spaces
        ArrayList<String[]> urlsInfos = new ArrayList<>();
        HashSet<Integer> urlIDs= new HashSet<>();
        
        //Get results
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken().toLowerCase();
            
            if(wordsIndex.containsKey(word)){
                //Get url ids
                HashSet<Integer> ids = wordsIndex.get(word);

                for (Integer id : ids) {

                    if(!urlIDs.contains(id)){ //Dont repeat url's
                        HashSet<String> idInfo = urlsIndex.get(id);
                        
                        String url = "";
                        String title = "";
                        String text = "";
            
                        for (String infoItem : idInfo) {
                            if (infoItem.startsWith("http")) { url = infoItem; }
                            else if (infoItem.startsWith("Title: ")) { title = infoItem; }
                            else { text = infoItem; }
                        }

                        int orderNumber = linksIndex.containsKey(url) ? linksIndex.get(url).size() : 0;

                        //Crop text
                        String croppedText= findPhrase(text, words);

                        //Improvement: if url has all words, has priority
                        if((text.toLowerCase()).contains(nsWords.toLowerCase())){ orderNumber += 100; } //text has priority
                        if((title.toLowerCase()).contains(nsWords.toLowerCase())){ orderNumber += 100; } //title has more
                    
                        String[] allInfos = new String[]{url, title, croppedText, String.valueOf(orderNumber)};
                        urlsInfos.add(allInfos);
                        urlIDs.add(id);
                    }
                }  
            }
        }

        //Order results
        urlsInfos.sort(Comparator.comparingInt(arr -> Integer.parseInt(((String[])arr)[3])).reversed());

        return urlsInfos;
    }


    //Search links
    /**
     * Search for links within the indexed URLs. Get
     * all urls that lead to the link passed as a param.
     * @param link The link to search for.
     * @return A list of string arrays containing URLs.
     */
    @Override
    public ArrayList<String[]> SearchLinks(String link){
        
        ArrayList<String[]> urls = new ArrayList<>();
        HashSet<Integer> urlIDs= new HashSet<>();
        
        //Get results
        if(linksIndex.containsKey(link)){
            //Get url ids
            HashSet<Integer> ids = linksIndex.get(link);

            for (Integer id : ids) {
                if(!urlIDs.contains(id)){ //Dont repeat url's
                    HashSet<String> idInfo = urlsIndex.get(id);
                    
                    String url = "";
                    for (String infoItem : idInfo) {
                        if (infoItem.startsWith("http")) { url = infoItem; }
                    }

                    urls.add(new String[]{url});
                    urlIDs.add(id);
                }
            }  
        }

        return urls;
    }


    //Save to file
    /**
     * Save inverted indexes data to a file specific to the barrel.
     * @param ID The ID of the Barrel.
     */
    public void SaveFile(int ID){
        String filename= "barrel" + String.valueOf(ID) + ".ser";
        File file = new File(filename);
        
        try {
            //Check if file exists
            if (!file.exists()) {
                //If it does not, create one
                if (!file.createNewFile()) {
                    System.err.println("Failed to create the file: " + filename);
                    return;
                }
                System.out.println("File created");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        //Serialization
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(wordsIndex);
            oos.writeObject(urlsIndex);
            oos.writeObject(linksIndex);
            System.out.println("Saved in file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Load from file
    /**
     * Load indexes data from a file (usually
     * from the barrel's previous file).
     * @param ID The ID of the Barrel.
     */
    public void LoadFile(int ID){

        //File name
        String filename= "barrel" + String.valueOf(ID) + ".ser";

        //Check if file exists
        File file = new File(filename);
        if (file.exists()) {
            //Deserialization
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                wordsIndex = (HashMap<String, HashSet<Integer>>) ois.readObject();
                urlsIndex = (HashMap<Integer, HashSet<String>>) ois.readObject();
                linksIndex = (HashMap<String, HashSet<Integer>>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("New Barrel or file not found\n");
        }
    }


    //Main
    /**
     * Main method to create and run a Barrel instance.
     * It connects the barrel to the Gateway's Barrel RMI.
     * And it detects ctrl-c shutdown. In that case, saves
     * the barrels infos and alerts the Gateway.
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {

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
            //Create barrel
            Barrel client = new Barrel();

            //RMI Gateway
            IRMIBarrel b = (IRMIBarrel) Naming.lookup("rmi://" + RMI_DATA + "/BARREL");
            int idBarrel= b.NewBarrel(client);

            //Detect Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        //If it crashes, save data
                        client.SaveFile(idBarrel);
                        //And alert gateway
                        b.RipBarrel(idBarrel);
                    } catch (RemoteException e) { e.printStackTrace(); }
                }
            });

            //Do checks --------------------------
            //Load from file (restore status)
            client.LoadFile(idBarrel);

            //Run barrel
            client.run(client, idBarrel, b);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}