/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.program.campaign;

import eds.component.data.IncompleteDataException;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import seca2.jsf.custom.messenger.FacesMessenger;
import seca2.program.FormEditEntity;
import segmail.component.campaign.CampaignService;
import segmail.entity.campaign.Campaign;

/**
 *
 * @author LeeKiatHaw
 */
@RequestScoped
@Named("FormCampaignSenderAttribute")
public class FormCampaignSenderAttribute implements FormEditEntity {

    @Inject ProgramCampaign program;
    @Inject
    FormProgramModeSwitch formSwitch;
    
    @EJB
    CampaignService campaignService;
    
    public Campaign getEditingCampaign() {
        return program.getEditingCampaign();
    }

    public void setEditingCampaign(Campaign editingCampaign) {
        program.setEditingCampaign(editingCampaign);
    }
    
    @Override
    public void saveAndContinue() {
        try {
            campaignService.updateCampaign(this.getEditingCampaign());
            reloadProgramToolbar();
            FacesMessenger.setFacesMessage(getClass().getSimpleName(), FacesMessage.SEVERITY_FATAL, "Campaign updated!", "");
        } catch (IncompleteDataException ex) {
            FacesMessenger.setFacesMessage(this.getClass().getSimpleName(), FacesMessage.SEVERITY_ERROR, ex.getMessage(), "");
        }
    }

    @Override
    public void saveAndClose() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void closeWithoutSaving() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void reloadProgramToolbar() {
        formSwitch.reloadCampaign();
        formSwitch.initEditCampaignMode();
        formSwitch.modifySessionContainer();
    }
    
}