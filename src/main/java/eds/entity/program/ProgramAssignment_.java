/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eds.entity.program;

import eds.entity.data.EnterpriseRelationship_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 *
 * @author KH
 */
@StaticMetamodel(ProgramAssignment.class)
public class ProgramAssignment_ extends EnterpriseRelationship_ {
    public static volatile SingularAttribute<ProgramAssignment,Boolean> DEFAULT_ASSIGNMENT;
}
