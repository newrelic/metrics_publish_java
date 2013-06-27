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
        ComponentData component = context.createComponent();
        component.guid = "com.test.guid";
        component.name = "test component name";
        
        // test that size is 1 and only one component was added
        int size = 0;
        ComponentData itrComponent = null; 
        Iterator<ComponentData> itr = context.getComponents();
        while (itr.hasNext()) {
            ++size;
            itrComponent = itr.next();
        }
        
        assertTrue(size == 1);
        assertTrue(itrComponent != null);
        assertEquals("com.test.guid", itrComponent.guid);
        assertEquals("test component name", itrComponent.name);
    }
}
