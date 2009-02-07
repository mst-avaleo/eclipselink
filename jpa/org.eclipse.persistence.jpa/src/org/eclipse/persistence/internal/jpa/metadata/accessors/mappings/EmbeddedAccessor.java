/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 *     09/23/2008-1.1 Guy Pelletier 
 *       - 241651: JPA 2.0 Access Type support
 *     01/28/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 1)
 *     02/06/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 2)
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;

import org.eclipse.persistence.internal.jpa.metadata.columns.AssociationOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.AttributeOverrideMetadata;

import org.eclipse.persistence.mappings.AggregateObjectMapping;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.EmbeddableMapping;

/**
 * An embedded relationship accessor. It may define all the same attributes
 * as an entity, therefore, it also must handle nesting embedded's to the nth
 * level. An embedded owning descriptor is a reference back to the actual
 * owning entity's descriptor where the first embedded was discovered.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class EmbeddedAccessor extends MappingAccessor {
    private List<AssociationOverrideMetadata> m_associationOverrides;
    private List<AttributeOverrideMetadata> m_attributeOverrides;

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public EmbeddedAccessor() {
        super("<embedded>");
    }
    
    /**
     * INTERNAL:
     */
    protected EmbeddedAccessor(String xmlElement) {
        super(xmlElement);
    }
    
    /**
     * INTERNAL:
     */
    public EmbeddedAccessor(Annotation embedded, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(embedded, accessibleObject, classAccessor);
        
        // Set the attribute overrides if some are present.
        m_attributeOverrides = new ArrayList<AttributeOverrideMetadata>();
        
        // Process the attribute overrides first.
        Annotation attributeOverrides = getAnnotation(AttributeOverrides.class);
        if (attributeOverrides != null) {
            for (Annotation attributeOverride : (Annotation[]) MetadataHelper.invokeMethod("value", attributeOverrides)) {
                m_attributeOverrides.add(new AttributeOverrideMetadata(attributeOverride, accessibleObject));
            }
        }
        
        // Process the single attribute override second.
        Annotation attributeOverride = getAnnotation(AttributeOverride.class);  
        if (attributeOverride != null) {
            m_attributeOverrides.add(new AttributeOverrideMetadata(attributeOverride, accessibleObject));
        }
        
        // Set the association overrides if some are present.
        m_associationOverrides = new ArrayList<AssociationOverrideMetadata>();
        
        // Process the attribute overrides first.
        Annotation associationOverrides = getAnnotation(AssociationOverrides.class);
        if (associationOverrides != null) {
            for (Annotation associationOverride : (Annotation[]) MetadataHelper.invokeMethod("value", associationOverrides)) {
                m_associationOverrides.add(new AssociationOverrideMetadata(associationOverride, accessibleObject));
            }
        }
        
        // Process the single attribute override second.
        Annotation associationOverride = getAnnotation(AssociationOverride.class);  
        if (associationOverride != null) {
            m_associationOverrides.add(new AssociationOverrideMetadata(associationOverride, accessibleObject));
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<AssociationOverrideMetadata> getAssociationOverrides() {
        return m_associationOverrides;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<AttributeOverrideMetadata> getAttributeOverrides() {
        return m_attributeOverrides;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject) {
        super.initXMLObject(accessibleObject);
    
        // Initialize lists of ORMetadata objects.
        initXMLObjects(m_attributeOverrides, accessibleObject);
        initXMLObjects(m_associationOverrides, accessibleObject);
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean isEmbedded() {
        return true;
    }
    
    /**
     * INTERNAL:
     * Process an embedded.
     */    
    public void process() {
        // Build and aggregate object mapping and add it to the descriptor.
        AggregateObjectMapping mapping = new AggregateObjectMapping();
        mapping.setIsReadOnly(false);
        mapping.setIsNullAllowed(true);
        mapping.setReferenceClassName(getReferenceClassName());
        mapping.setAttributeName(getAttributeName());    
        
        // Will check for PROPERTY access
        setAccessorMethods(mapping);
        
        // Process attribute overrides.
        processAttributeOverrides(mapping);
       
        // Process association overrides (this is an annotation only thing).
        processAssociationOverrides(mapping);
        
        // Process a @ReturnInsert and @ReturnUpdate (to log a warning message)
        processReturnInsertAndUpdate();
        
        // Add the mapping to the descriptor and we are done.
        addMapping(mapping);
    }

    /**
     * INTERNAL:
     * Process the association overrides for the given embeddable mapping which
     * is either an embedded or element collection mapping. Association 
     * overrides are used to apply the correct field name translations of 
     * foreign key fields.
     */
    protected void processAssociationOverrides(EmbeddableMapping mapping) {
        // Process the list of association overrides for this mapping.
        for (AssociationOverrideMetadata associationOverride : processAssociationOverrides(m_associationOverrides).values()) {
            processAssociationOverride(associationOverride, mapping, getReferenceDescriptor().getMappingForAttributeName(associationOverride.getName()), getOwningDescriptor().getPrimaryTable(), getReferenceDescriptor());
        }
    }
    
    /**
     * INTERNAL:
     * Process the attribute overrides for the given embedded mapping. Attribute 
     * overrides are used to apply the correct field name translations of direct 
     * fields.
     */
    protected void processAttributeOverrides(AggregateObjectMapping aggregateObjectMapping) {
        // Process the list of attribute overrides for this mapping. This
        // includes the immediate specifications (the ones defined on the 
        // mapping accessor) and those descriptor level ones that were defined
        // for this accessor.
        for (AttributeOverrideMetadata attributeOverride : processAttributeOverrides(m_attributeOverrides).values()) {
            // The getMappingForAttributeName will take care of any dot 
            // notation attribute names when looking for the mapping. It will
            // traverse the embeddable chain. 
            String attributeName = attributeOverride.getName();
            DatabaseMapping mapping = getReferenceDescriptor().getMappingForAttributeName(attributeName);
                
            if (mapping == null) {
                throw ValidationException.embeddableAttributeOverrideNotFound(getReferenceDescriptor().getJavaClass(), attributeName, getJavaClass(), getAttributeName());
            } else if (! mapping.isDirectToFieldMapping()) {
                throw ValidationException.invalidEmbeddableAttributeForAttributeOverride(getReferenceDescriptor().getJavaClass(), attributeName, getJavaClass(), getAttributeName());
            } else {
                addFieldNameTranslation(aggregateObjectMapping, attributeName, attributeOverride.getColumn().getDatabaseField(), mapping);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setAssociationOverrides(List<AssociationOverrideMetadata> associationOverrides) {
        m_associationOverrides = associationOverrides;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setAttributeOverrides(List<AttributeOverrideMetadata> attributeOverrides) {
        m_attributeOverrides = attributeOverrides;
    }
}
