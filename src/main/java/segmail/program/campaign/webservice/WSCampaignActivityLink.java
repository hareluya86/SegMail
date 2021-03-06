/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.program.campaign.webservice;

import eds.component.data.EntityNotFoundException;
import eds.component.data.IncompleteDataException;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import seca2.bootstrap.module.Client.ClientContainer;
import seca2.component.landing.LandingService;
import segmail.component.campaign.CampaignExecutionService;
import segmail.component.campaign.CampaignService;
import segmail.entity.campaign.link.CampaignActivityOutboundLink;
import segmail.program.campaign.ProgramCampaign;

/**
 * Not in use anymore, since we want to full utilize JSF partial requests.
 * 
 * @author LeeKiatHaw
 */
@Deprecated
@WebService(serviceName = "WSCampaignActivityLink")
@HandlerChain(file = "handlers-server.xml")
public class WSCampaignActivityLink {

    @EJB CampaignService campService;
    @EJB CampaignExecutionService campExeService;
    @EJB LandingService landingService;
    
    @Inject ClientContainer cltCont; 
    
    /**
     * If this is not injected, no service calls can proceed.
     * 
     */
    @Inject ProgramCampaign program;
    /**
     * 
     * @param linkTarget
     * @param linkText
     * @param index
     * @param originalHTML
     * @return a generated redirect link
     * @throws eds.component.data.IncompleteDataException
     */
    @WebMethod(operationName = "createOrUpdateLink")
    public String createOrUpdateLink(
            @WebParam(name = "linkTarget") String linkTarget, 
            @WebParam(name = "linkText") String linkText, 
            @WebParam(name = "index") int index
            ) throws IncompleteDataException {
        
        if(program == null || program.getEditingCampaignId() <= 0)
            throw new RuntimeException("Program is not set, this service was not called from the correct page.");
        
        CampaignActivityOutboundLink link = campService.createOrUpdateLink(
                program.getEditingActivity(), 
                linkTarget, 
                linkText,
                index);
        
        String redirectLink = campService.constructLink(link);
        
        return redirectLink;
    }
    
    /**
     * 
     * @param linkKey unique identifier of the CampaignActivityOutboundLink
     * @param emailKey unique identifier of the source Email
     * @return
     * @throws EntityNotFoundException 
     */
    @WebMethod(operationName = "redirectAndUpdate")
    public String redirectAndUpdate(
            @WebParam(name = "linkKey") String linkKey, 
            @WebParam(name = "emailKey") String emailKey ) throws EntityNotFoundException {
        String redirectLink = campExeService.getRedirectLinkAndUpdateHit(linkKey,emailKey);
        
        return redirectLink;
    }
    
    /**
     * 
     * @param redirectLink
     * @return count of link clicks or -1 if no such link is found
     */
    @WebMethod(operationName = "getClickCountForActivity")
    public long getClickCountForActivity(@WebParam(name = "redirectLink") String redirectLink) {
        //Get the key from the link
        if(redirectLink == null || redirectLink.isEmpty())
            return -1;
        
        String[] split = redirectLink.split("/");
        if(split == null || split.length <=0)
            return -1;
        
        String key = split[split.length-1];
        
        long result = campService.getLinkClicks(key);
        
        return result;
    }
    
    @WebMethod(operationName = "getExecutedForCampaignActivity")
    public long getExecutedForCampaignActivity(@WebParam(name = "campaignActivityId") long campaignActivityId) {
        long result = campService.countEmailsSentForActivity(campaignActivityId);
        return result;
    }
    
    @WebMethod(operationName = "getTotalTargetedForCampaignActivity")
    public long getTotalTargetedForCampaignActivity(@WebParam(name = "campaignActivityId") long campaignActivityId) {
        long result = campService.countTargetedSubscribersForActivity(campaignActivityId,cltCont.getClient().getOBJECTID());
        return result;
    }
    
    
}
