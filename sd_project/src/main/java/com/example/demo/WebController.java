package com.example.demo;

import java.rmi.*;

import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.thymeleaf.expression.Arrays;
import java.util.Map;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentResponse;

@Controller
public class WebController{

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String PROJECT_ID = "sdgoogol2024";
    private static final String LOCATION = "europe-west2";

    @Resource(name = "adminPage")
    private WebControllerBean bean;

    IRMIClient gateway;

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    WebControllerBean adminPage() {

        try {
            bean = new WebControllerBean(
            );
            String RMI_DATA= "localhost:1099";
            gateway = (IRMIClient) Naming.lookup("rmi://" + RMI_DATA + "/GATEWAY");
            gateway.NewServer(bean);
            return bean;
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    @GetMapping("/")
    public String redirect() {
        return "redirect:/googol";
    }

    @GetMapping("/googol")
    public String searchSubmission() {
        System.out.println(bean);
        return "googol";
    }

    @GetMapping("/results")
    public String displayResults(@RequestParam("query") String query, @RequestParam("searchType") int searchType,
    @RequestParam(value = "searchPage", required = false, defaultValue = "1") int searchPage, Model model) {

        model.addAttribute("query", query);
        model.addAttribute("searchType", searchType);
        model.addAttribute("searchPage", searchPage);

        //Search with barrels - - - -
        ArrayList<String[]> allResults= new ArrayList<>();
        
        try {
            switch (searchType) {
                case 1:
                    allResults = gateway.sendMessage(query, 1);
                    break;
                case 2:
                
                    //Gemini api - - - -
                    try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION);) {
                
                        GenerativeModel gemModel = new GenerativeModel("gemini-pro", vertexAi);
                        String prompt= "Tell me a brief description of the term(s) " + query + ", make it purely descriptive, without sounding like a response or talk like humans.";
                
                        GenerateContentResponse response = gemModel.generateContent(prompt);
                        String responseText= response.getCandidates(0).getContent().getParts(0).getText();
                        allResults.add(new String[] {responseText});

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    ArrayList<String[]> barrelResults = gateway.sendMessage(query, 2);
                    allResults.addAll(barrelResults);
                    
                    //HackerNews api - - - -
                    try {
                        //Fetch top stories IDs
                        String[] topStoriesIds = restTemplate.getForObject("https://hacker-news.firebaseio.com/v0/topstories.json", String[].class);
                        if (topStoriesIds != null) {
                            int counter=0;

                            for (String id : topStoriesIds) {
                                //Get only first 100 stories
                                if(counter == 10){
                                    break;
                                }

                                //Fetch story details
                                String storyUrl = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json", id);
                                Map<String, Object> story = restTemplate.getForObject(storyUrl, Map.class);

                                if (story != null) {
                                    String title = (String) story.get("title");
                                    System.out.println(title);
                                    String text = (String) story.get("text");
                                    String url = (String) story.get("url");
                                    if ((title != null && title.contains(query)) || (text != null && text.contains(query))) {
                                        gateway.addStory(url);
                                    }
                                }

                                counter++;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case 3:
                    allResults = gateway.sendMessage(query, 3);
                    break;
                default:
                    break;
            }

            //Separate searches by pages
            ArrayList<String[]> searchResults= new ArrayList<>();

            for (int i = (searchPage-1)*10; i < searchPage*10; i++) {
                if (allResults.size() > i){
                    searchResults.add(allResults.get(i));
                } else{ break; }
            }
            
            model.addAttribute("searchResults", searchResults);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return "results";
    }
}