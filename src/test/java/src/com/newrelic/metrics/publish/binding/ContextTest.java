package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

public class ContextTest {

    @Test
    public void testCreateComponent() {
        
        // test component
        Context context = new Context();
        ComponentData firstComponent = context.createComponent();
        firstComponent.guid = "com.test.guid.first";
        firstComponent.name = "first test component name";
        
        ComponentData secondComponent = context.createComponent();
        secondComponent.guid = "com.test.guid";
        secondComponent.name = "test component name";
        
        // test that only 2 components are added in the correct order
        int size = 0;
        ComponentData itrComponent = null; 
        Iterator<ComponentData> itr = context.getComponents();
        while (itr.hasNext()) {
            ++size;
            itrComponent = itr.next();
        }
        
        assertTrue(size == 2);
        assertTrue(itrComponent != null);
        assertEquals("com.test.guid", itrComponent.guid);
        assertEquals("test component name", itrComponent.name);
    }
}
