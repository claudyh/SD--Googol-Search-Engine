//RMI Client -------------------
package com.example.demo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


/**
 * Represents an RMI bridge between the Gateway and the Clients.
 */
public class RMIClient extends UnicastRemoteObject implements IRMIClient {

    private static Gateway g;
    private static IWebController ws;
    private ArrayList<String[]> result;
    private HashMap<String, Integer> commun10;


    //RMI Client
    /**
     * Constructs a new RMIClient instance.
     * Creates "top 10 searches" hashmap.
     * @throws RemoteException if an RMI error occurs.
     */
    public RMIClient() throws RemoteException {
        super();
        commun10= new HashMap<>();
    }


    //Gateway
    /**
     * Adds the current Gateway.
     * @param inputGateway The Gateway to add.
     * @throws RemoteException if an RMI error occurs.
     */
    public void AddGateway(Gateway inputGateway) throws RemoteException {
        g= inputGateway;
    }


    //WebServer
    /**
     * Adds the current WebServer.
     * @param server The Server to add.
     * @throws RemoteException if an RMI error occurs.
     */
    public void NewServer(IWebController server) throws RemoteException {
        System.out.println("\n\n\nESTOU A ADICIONAR O SERVER\n\n\n");
        ws= server;
    }


    //HackerNews urls
    /**
     * Adds hackernews urls to url queue.
     * @param url The url to add.
     */
    public void addStory(String url) throws RemoteException {
        
        //Check if its valid
        if(url != null && url.toLowerCase().startsWith("http")){
            System.out.println("Hackernews url added: " + url);
            try {
                //Add to queue
                g.AddToQueue(url);
            } catch (Exception e) { e.printStackTrace(); }
        } 
    }


    //Admin Page
    /**
     * Gives infos of admin page in real time.
     */
    public void AdminPage() throws RemoteException {

        //ADMIN PAGE - get top10 and ask barrels info
        System.out.println("ADMIN PAGE!");

        //10 pesquisas mais comuns
        ArrayList<String[]> resultAdmin= new ArrayList<>();
        resultAdmin.add(new String[] { "Top 10 searches:" });
        
        List<String> keyList = new ArrayList<>(commun10.keySet());
        keyList.sort(Comparator.comparingInt(commun10::get).reversed());

        int count = 0;
        for (String key : keyList) {
            if (count >= 10) {
                break;
            }

            resultAdmin.add(new String[] { key, Integer.toString(commun10.get(key)) });
            count++;
        }
        
        if(resultAdmin.size() == 1){
            resultAdmin.add(new String[] { "No searches found" });
        }
        
        resultAdmin.add(new String[] { "\nActive Barrels:" });

        ArrayList<String[]> resultBarrels = g.BarrelsData();
        resultAdmin.addAll(resultBarrels);
        result= resultAdmin;

        //Send result
        if(ws != null){
            ws.AdminPageWarning(result);
        } else{
            System.out.println("\nESTOU NULL\n");
        }
    }


    //Top 10 searches
    /**
     * Adds words and their count to the 10
     * most common search terms hashmap.
     * @param word The word to add.
     */
    private void AddTop10Words(String word) {
        //Check if word exists the hashmap 10commun
        if (commun10.containsKey(word)) {
            int count = commun10.get(word);
            commun10.put(word, count + 1);
        } else {
            //Put the word
            commun10.put(word, 1);          
        }
    }


    //Send message
    /**
     * Sends a message to the Gateway and, based on the given option,
     * performs checks and gets the results asked. It can perform a
     * search by words and url's, add url's to the Queue and list the
     * Admin's page information (top 10 searches, list of barrels,..).
     * @param message The message to send.
     * @param option The option indicating the type of message.
     * @return An ArrayList containing the server's response.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public ArrayList<String[]> sendMessage(String message, int option) throws RemoteException {

        //Gateway logic
        switch (option) {
            case 1:
                //URL - put on queue

                //Check
                if(!message.toLowerCase().startsWith("http")){
                    ArrayList<String[]> auxResult = new ArrayList<>();
                    auxResult.add(new String[]{"URL not valid"});
                    result= auxResult;
                    break;
                }
                
                System.out.println("E UM URL! " + message);
                g.AddToQueue(message);

                //Result
                ArrayList<String[]> auxResult = new ArrayList<>();
                auxResult.add(new String[]{"URL added"});
                result= auxResult;
                break;

            case 2:
                //WORDS - ask to barrels
                System.out.println("E UMA WORD! " + message);
                AddTop10Words(message);
                result= g.SearchMessage(message);
                break;
            
            case 3:
                //LINKS - ask to barrels

                //Check
                if(!message.toLowerCase().startsWith("http")){
                    ArrayList<String[]> resultAux = new ArrayList<>();
                    resultAux.add(new String[]{"URL not valid"});
                    result= resultAux;
                    break;
                }

                System.out.println("QUER OS LINKS!");
                result= g.GetLinks(message);
                break;
        }

        //Send to client
        return result;
    }
}
