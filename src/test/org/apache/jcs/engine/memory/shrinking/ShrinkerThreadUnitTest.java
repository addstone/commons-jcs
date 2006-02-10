package org.apache.jcs.engine.memory.shrinking;

import junit.framework.TestCase;

import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.CompositeCacheAttributes;
import org.apache.jcs.engine.ElementAttributes;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.memory.MemoryCacheMockImpl;

/**
 * This tests the functionality of the shrinker thread.
 * 
 * @author Aaron Smuts
 * 
 */
public class ShrinkerThreadUnitTest
    extends TestCase
{

    /**
     * Setup cache attributes in mock. Create the shrinker with the mock. Add
     * some elements into the mock memory cache see that they get spooled.
     * 
     * @throws Exception
     * 
     */
    public void testSimpleShrink()
        throws Exception
    {
        MemoryCacheMockImpl memory = new MemoryCacheMockImpl();

        CompositeCacheAttributes cacheAttr = new CompositeCacheAttributes();
        cacheAttr.setMaxMemoryIdleTimeSeconds( 1 );
        cacheAttr.setMaxSpoolPerRun( 10 );

        memory.setCacheAttributes( cacheAttr );

        String key = "key";
        String value = "value";

        ICacheElement element = new CacheElement( "testRegion", key, value );

        ElementAttributes elementAttr = new ElementAttributes();
        elementAttr.setIsEternal( false );
        element.setElementAttributes( elementAttr );
        element.getElementAttributes().setMaxLifeSeconds( 1 );
        memory.update( element );

        ICacheElement returnedElement1 = memory.get( key );
        assertNotNull( "We should have received an element", returnedElement1 );

        // set this to 2 seconds ago.
        elementAttr.lastAccessTime = System.currentTimeMillis() - 2000;

        ShrinkerThread shrinker = new ShrinkerThread( memory );
        Thread runner = new Thread( shrinker );
        runner.run();

        Thread.sleep( 500 );

        ICacheElement returnedElement2 = memory.get( key );
        assertTrue( "Waterfall should have been called.", memory.waterfallCallCount > 0 );
        assertNull( "We not should have received an element.  It should have been spooled.", returnedElement2 );
    }

    /**
     * Add 10 to the memory cache. Set the spool per run limit to 3.
     * 
     * @throws Exception
     */
    public void testSimpleShrinkMutiple()
        throws Exception
    {
        MemoryCacheMockImpl memory = new MemoryCacheMockImpl();

        CompositeCacheAttributes cacheAttr = new CompositeCacheAttributes();
        cacheAttr.setMaxMemoryIdleTimeSeconds( 1 );
        cacheAttr.setMaxSpoolPerRun( 3 );

        memory.setCacheAttributes( cacheAttr );

        for ( int i = 0; i < 10; i++ )
        {
            String key = "key" + i;
            String value = "value";

            ICacheElement element = new CacheElement( "testRegion", key, value );

            ElementAttributes elementAttr = new ElementAttributes();
            elementAttr.setIsEternal( false );
            element.setElementAttributes( elementAttr );
            element.getElementAttributes().setMaxLifeSeconds( 1 );
            memory.update( element );

            ICacheElement returnedElement1 = memory.get( key );
            assertNotNull( "We should have received an element", returnedElement1 );

            // set this to 2 seconds ago.
            elementAttr.lastAccessTime = System.currentTimeMillis() - 2000;
        }

        ShrinkerThread shrinker = new ShrinkerThread( memory );
        Thread runner = new Thread( shrinker );
        runner.run();

        Thread.sleep( 500 );

        assertEquals( "Waterfall called the wrong number of times.", 3, memory.waterfallCallCount );
        
        assertEquals( "Wrong number of elements remain.", 7, memory.getSize() );
    }

}