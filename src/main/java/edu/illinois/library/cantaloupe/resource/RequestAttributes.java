package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.OperationList;

import java.util.HashMap;
import java.util.Map;

public class RequestAttributes {

    private String clientIp;
    private Map<String,String> headers = new HashMap<>();
    private OperationList opList;

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public OperationList getOperationList() {
        return opList;
    }

    public void setOperationList(OperationList opList) {
        this.opList = opList;
    }

}
