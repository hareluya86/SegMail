/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.program.campaign;

import eds.component.data.EntityNotFoundException;
import eds.component.data.IncompleteDataException;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import seca2.bootstrap.UserRequestContainer;
import seca2.bootstrap.module.Client.ClientContainer;
import seca2.jsf.custom.messenger.FacesMessenger;
import seca2.program.FormCreateEntity;
import segmail.component.campaign.CampaignService;
import segmail.entity.campaign.ACTIVITY_TYPE;
import segmail.entity.campaign.Campaign;
import segmail.entity.campaign.CampaignActivity;

/**
 *
 * @author LeeKiatHaw
 */
@RequestScoped
@Named("FormCreateNewEmailActivity")
public class FormCreateNewEmailActivity implements FormCreateEntity {
    @Inject UserRequestContainer reqCont;
    @Inject ClientContainer cltCont;
    
    @EJB CampaignService campaignService;
    
    @Inject ProgramCampaign program;
    
    private String activityName;
    private String activityGoals;

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getActivityGoals() {
        return activityGoals;
    }

    public void setActivityGoals(String activityGoals) {
        this.activityGoals = activityGoals;
    }
    
    public void setEditingCampaign(Campaign editingCampaign) {
        program.setEditingCampaign(editingCampaign);
    }

    public Campaign getEditingCampaign() {
        return program.getEditingCampaign();
    }
    
    public boolean renderThis(){
        return reqCont.getProgramParamsOrdered().size() == 1;
    }
    
    @Override
    public void createNew() {
        try {
            CampaignActivity newActivity = campaignService.createCampaignActivity(
                    getEditingCampaign().getOBJECTID(),
                    cltCont.getClient().getOBJECTID(),
                    activityName, 
                    activityGoals, 
                    ACTIVITY_TYPE.EMAIL);
            FacesMessenger.setFacesMessage(ProgramCampaign.class.getSimpleName(), FacesMessage.SEVERITY_FATAL, "Activity created. You can edit your activity now.", "");
            program.refresh();
        } catch (IncompleteDataException ex) {
            FacesMessenger.setFacesMessage(this.getClass().getSimpleName(), FacesMessage.SEVERITY_ERROR, ex.getMessage(), "");
        } catch (EntityNotFoundException ex) {
            FacesMessenger.setFacesMessage(this.getClass().getSimpleName(), FacesMessage.SEVERITY_ERROR, ex.getMessage(), "");
        } catch (EJBException ex) {
            FacesMessenger.setFacesMessage(this.getClass().getSimpleName(), FacesMessage.SEVERITY_ERROR, ex.getCause().getMessage(), "");
        }
    }

    @Override
    public void cancel() {
        
    }
    
}
