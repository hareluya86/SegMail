/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seca2.jsf.custom.dropdown;

import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UISelectOne;

/**
 * It seems better to use a composite component than a custom one, since the 
 * components are already available - h:selectOneMenu, f:selectItem, etc.
 * @author LeeKiatHaw
 */
@FacesComponent(DropdownComponent.COMPONENT_TYPE)
public class DropdownComponent extends UISelectOne implements NamingContainer {

    public static final String COMPONENT_FAMILY = "Dropdown";
    public static final String COMPONENT_TYPE = "Dropdown";
    
    //Properties
    public static final String EMPTY_MESSAGE = "emptyMessage";
    public static final String EMPTY_MESSAGE_DEFAULT = "No Item exists yet";
    
    public static final String STYLECLASS = "styleClass";
    public static final String STYLECLASS_DEFAULT = ""; 
    
    public static final String BUTTON_CLASS = "buttonClass";
    public static final String BUTTON_CLASS_DEFAULT = "btn dropdown-toggle";
    
    public static final String BUTTON_COLOR = "buttonColor";
    public static final String BUTTON_COLOR_DEFAULT = "default";
    
    public static final String ARROW_CLASS = "arrowClass";
    public static final String ARROW_CLASS_DEFAULT = "dropdown-arrow";
    
    public static final String LIST_CLASS = "listClass";
    public static final String LIST_CLASS_DEFAULT = "dropdown-menu";
    
    public static final String ADD_LIST_CLASS = "addListClass";
    public static final String ADD_LIST_CLASS_DEFAULT = "";
    
    //public static final String LIST_ITEMS = "listItems";
    

    public DropdownComponent() {
        
    }
    
    @Override
    public String getFamily() {
        return DropdownComponent.COMPONENT_FAMILY; 
    }
    
    public String getEmptyMessage(){
        return (String) this.getStateHelper().eval(EMPTY_MESSAGE, EMPTY_MESSAGE_DEFAULT);
    }
    
    public void setEmptyMessage(String emptyMessage){
        this.getStateHelper().put(EMPTY_MESSAGE, emptyMessage);
    }
    
    public String getStyleClass(){
        return (String) this.getStateHelper().eval(STYLECLASS, STYLECLASS_DEFAULT);
    }
    
    public void setStyleClass(String StyleClass){
        this.getStateHelper().put(STYLECLASS, StyleClass);
    }
    
    public String getButtonClass(){
        return (String) this.getStateHelper().eval(BUTTON_CLASS, BUTTON_CLASS_DEFAULT);
    }
    
    public void setButtonClass(String buttonClass){
        this.getStateHelper().put(BUTTON_CLASS, buttonClass);
    }
    
    public String getArrowClass(){
        return (String) this.getStateHelper().eval(ARROW_CLASS, ARROW_CLASS_DEFAULT);
    }
    
    public void setArrowClass(String arrowClass){
        this.getStateHelper().put(ARROW_CLASS, arrowClass);
    }
    
    public String getListClass(){
        return (String) this.getStateHelper().eval(LIST_CLASS, LIST_CLASS_DEFAULT);
    }
    
    public void setListClass(String listClass){
        this.getStateHelper().put(LIST_CLASS, listClass);
    }
    
    public String getAddListClass(){
        return (String) this.getStateHelper().eval(ADD_LIST_CLASS, ADD_LIST_CLASS_DEFAULT);
    }
    
    public void setAddListClass(String addListClass){
        this.getStateHelper().put(ADD_LIST_CLASS, addListClass);
    }
    
    /*
    public List getListItems(){
        return (List) this.getStateHelper().eval(LIST_ITEMS, new ArrayList());
    }
    
    public void setListItems(List listItems){
        this.getStateHelper().put(LIST_ITEMS, listItems);
    }
    */
    
    public String getButtonColor(){
        return (String) this.getStateHelper().eval(BUTTON_COLOR, BUTTON_COLOR_DEFAULT);
    }
    
    public void setButtonColor(String color){
        this.getStateHelper().put(BUTTON_COLOR, color);
    }
}
