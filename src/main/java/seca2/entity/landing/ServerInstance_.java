/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seca2.entity.landing;

import eds.entity.data.EnterpriseObject_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 *
 * @author LeeKiatHaw
 */
@StaticMetamodel(ServerInstance.class)
public class ServerInstance_ extends EnterpriseObject_ {
    public static volatile SingularAttribute<ServerInstance,String> NAME;
    public static volatile SingularAttribute<ServerInstance,String> IP_ADDRESS;
}
