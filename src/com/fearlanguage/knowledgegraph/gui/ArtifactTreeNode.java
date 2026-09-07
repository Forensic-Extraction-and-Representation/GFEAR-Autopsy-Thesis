/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.fearlanguage.knowledgegraph.gui;

import javax.swing.tree.DefaultMutableTreeNode;
import org.sleuthkit.datamodel.BlackboardArtifact.Type;

/**
 *
 * @author Allan
 */
public class ArtifactTreeNode extends DefaultMutableTreeNode {
    
    Type artifactType;
    public ArtifactTreeNode(Type artifactType){
        super(artifactType.getDisplayName());
        this.artifactType = artifactType;
    }
    
    public Type getArtifactType(){
        return artifactType;
    }
}
