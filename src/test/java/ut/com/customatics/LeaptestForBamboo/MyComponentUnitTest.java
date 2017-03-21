package ut.com.customatics.LeaptestForBamboo;

import org.junit.Test;
import com.customatics.LeaptestForBamboo.api.MyPluginComponent;
import com.customatics.LeaptestForBamboo.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}