/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seca2.component.landing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import seca2.entity.landing.ServerInstance;

/**
 * Currently only supports LandingServerGenerationStrategy.ROUND_ROBIN implementation.
 * 
 * @author LeeKiatHaw
 */
@ApplicationScoped
public class LandingServiceContainer {
    
    private Map<String,Integer> POINTER_NEXT = new HashMap<>();
    private int POINTER_OWN;
    private Map<String,List<ServerInstance>> SERVERS = new HashMap<>();
    
    public int getPOINTER_OWN() {
        return POINTER_OWN;
    }

    public void setPOINTER_OWN(int POINTER_OWN) {
        this.POINTER_OWN = POINTER_OWN;
    }
    
    public void addServer(ServerInstance server) {
        List<ServerInstance> servers = new ArrayList<ServerInstance>();
        servers.add(server);
        this.addServers(servers);
    }
    
    public void addServers(List<ServerInstance> servers) {
        for(ServerInstance server : servers) {
            //Initialize the List object
            if(!SERVERS.containsKey(server.getSERVER_NODE_TYPE())) {
                SERVERS.put(server.getSERVER_NODE_TYPE(), new ArrayList<ServerInstance>());
            }
            //Do not add duplicates
            if(SERVERS.get(server.getSERVER_NODE_TYPE()).contains(server)) {
                continue;
            }
            SERVERS.get(server.getSERVER_NODE_TYPE()).add(server);
        }
        resetPointers();
    }
    
    public void removeServer(long serverId) {
        ServerInstance server = new ServerInstance();
        server.setOBJECTID(serverId); //equals() will only compare type and OBJECT_ID;
        
        removeServer(server);
    }
    
    public void removeServer(ServerInstance server) {
        List<ServerInstance> servers = new ArrayList<ServerInstance>();
        servers.add(server);
        this.removeServers(servers);
    }
    
    public void removeServers(List<ServerInstance> servers) {
        for(List<ServerInstance> existingServers : SERVERS.values()) {
            existingServers.removeAll(servers);
        }
        resetPointers();
    }
    
    public ServerInstance getNextServerRR(ServerNodeType type) {
        //Initialize first
        if(!POINTER_NEXT.containsKey(type.value) || POINTER_NEXT.get(type.value) == null)
            POINTER_NEXT.put(type.value, 0);
        int pointer = POINTER_NEXT.get(type.value);
        int size = SERVERS.get(type.value).size();
        POINTER_NEXT.put(type.value, (pointer+1) % size);
        
        return SERVERS.get(type.value).get(pointer);
    }
    
    public void resetPointers() {
        for(Integer pointer : POINTER_NEXT.values()) {
            pointer = 0;
        }
    }
    
    public int getTotalServerCount() {
        int count = 0;
        for(List list : SERVERS.values()){
            count += list.size();
        }
        return count;
    }
}
