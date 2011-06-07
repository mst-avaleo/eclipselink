/*******************************************************************************
 * Copyright (c) 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Matt MacIvor - 2.4 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.binder.nullpolicy;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.persistence.oxm.annotations.XmlMarshalNullRepresentation;
import org.eclipse.persistence.oxm.annotations.XmlNullPolicy;

@XmlRootElement(name="employee")
public class EmployeeCompositeA {

    public String name;
    
    @XmlNullPolicy(nullRepresentationForXml=XmlMarshalNullRepresentation.EMPTY_NODE, emptyNodeRepresentsNull=true)
    public Address address;
    
    @XmlNullPolicy(nullRepresentationForXml=XmlMarshalNullRepresentation.EMPTY_NODE, emptyNodeRepresentsNull=true)
    public List<PhoneNumber> phone;
}
