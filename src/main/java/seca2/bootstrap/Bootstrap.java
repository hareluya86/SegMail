/**
 * This is the most important class in the entire application!!! It is a 
 * core that dispatches, load and manage key components of the application.
 * <ul>
 * <li>User Management</li>
 * <li>Program Management</li>
 * <li>Presentation Management</li>
 * </ul>
 * It is important that these parts operate independently of each other and they
 * can be changed/enhanced without having to change the others.
 * 
 */

package seca2.bootstrap;

import com.ocpsoft.pretty.faces.annotation.URLAction;
import com.ocpsoft.pretty.faces.annotation.URLActions;
import com.ocpsoft.pretty.faces.annotation.URLBeanName;
import com.ocpsoft.pretty.faces.annotation.URLMapping;
import com.ocpsoft.pretty.faces.annotation.URLMappings;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author vincent.a.lee
 */
@URLMappings(mappings={
    @URLMapping(id="home", pattern="/",viewId="/program/index.xhtml"),
    @URLMapping(id="program", pattern="/program/#{bootstrap.program}/",viewId="/program/index.xhtml"),
    @URLMapping(id="install", pattern="/install/",viewId="/program/programs/install/install.xhtml")
})
@URLBeanName("bootstrap")
@Named("bootstrap")
@RequestScoped
public class Bootstrap implements Serializable {
    
    /*@Inject*/ private DefaultValues defaultValues = new DefaultValues();
    /*@Inject*/ private DefaultSites defaultSites = new DefaultSites();
    /**
     * Use a generic Map object as a trial first to see what are the parameters 
     * that we will need.
     */
    @Inject private BootstrapInput input;
    @Inject private BootstrapOutput output;
    /**
     * Chain of Responsibility object that will execute the modules in sequence.
     */
    @Inject private BootstrappingChainFactory bootstrappingChain;
    /**
     * Injected view parameters
     */
    private String program; //which program are you loading
    
    @PostConstruct
    public void init(){
        //input = new BootstrapInput();
        //output = new BootstrapOutput();
        
        System.out.println(program);
        
    }
    
    @URLActions(actions={
        @URLAction(mappingId="home", onPostback=true),
        @URLAction(mappingId="program", onPostback=true)
    })
    public void startProcessing(){
        
        FacesContext fc = FacesContext.getCurrentInstance();
        input.setProgram(program);
        //input.setFacesContext(fc);
        input.setContextPath(fc.getExternalContext().getRequestContextPath());
        this.startChain(input,output);
        
        if(fc.getApplication().getProjectStage().equals(ProjectStage.Development)){
            System.out.println("Bootstrap processing is completed.");
        }
    }
    
    public void startChain(BootstrapInput inputContext, BootstrapOutput outputContext){
        //Start the chain
        BootstrapModule head = this.bootstrappingChain.getCoreHead();
        //Only process noncore modules if application is not in setup mode
        if(!inputContext.isSetup()){
            head.strapNextAtLastPos(this.bootstrappingChain.getNonCoreHead());
        }
        
        try{
            head.start(inputContext, outputContext);
        } catch(Exception ex){
            outputContext.setErrorMessage(ex.getMessage());
            outputContext.setTemplateRoot(defaultSites.ERROR_PAGE_TEMPLATE);
            outputContext.setPageRoot(defaultSites.ERROR_PAGE);
            
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            outputContext.setErrorStackTrace(sw.toString());
        }
        
        
    }

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public BootstrapOutput getOutput() {
        return output;
    }
    
    public void setOutput(BootstrapOutput output) {    
        this.output = output;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }
    
    
    public BootstrapInput getInput() {
        return input;
    }

    public void setInput(BootstrapInput input) {
        this.input = input;
    }
    // </editor-fold>

}
