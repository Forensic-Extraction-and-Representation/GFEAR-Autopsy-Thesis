/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.fearlanguage.knowledgegraph;

import java.util.HashMap;
import java.util.Map;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.TskCoreException;

/**
 *
 * @author Allan
 */
public class BlackboardArtifactHelpers {
    
    public static Map<String, Object> getAttributeListFromBlackboardArtifact(BlackboardArtifact ba) throws TskCoreException{
        Map<String, Object> attributes = new HashMap<>();
        getAttributeListFromBlackboardArtifact(ba, attributes);
        return attributes;
    }
     
    public static void getAttributeListFromBlackboardArtifact(BlackboardArtifact ba, Map<String, Object> attributes) throws TskCoreException{
        for(BlackboardAttribute attr : ba.getAttributes()){
            String key = attr.getAttributeType().getDisplayName();
            key = key.replaceAll("[^a-zA-Z0-9]", "");

            if (key.contains("Date") || key.contains("DateTime")) {
                key = key.replaceAll("DateTime|Date", "").trim();
                key += "DateTime";
            }
            
            attributes.put(key, getAttributeValue(attr));
        }
    }
    
    public static Object getAttributeValue(BlackboardAttribute attr){
        switch(attr.getValueType()){
            case STRING:
            case JSON:
                return attr.getValueString();
            case BYTE:
                return attr.getValueBytes();
            case LONG:
            case DATETIME:
                return attr.getValueLong();
            case DOUBLE:
                return attr.getValueDouble();
            case INTEGER:
                return attr.getValueInt();
            default:
                return null;
        }
    }
}
