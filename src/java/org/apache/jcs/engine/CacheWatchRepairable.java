package org.apache.jcs.engine;

import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.Map.Entry;

import org.apache.jcs.engine.behavior.ICacheListener;
import org.apache.jcs.engine.behavior.ICacheObserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Interceps the requests to the underlying ICacheObserver object so that the
 * listeners can be recorded locally for remote connection recovery purposes.
 * (Durable subscription like those in JMS is not implemented at this stage for
 * it can be too expensive on the runtime.)
 *
 * @author asmuts
 * @created January 15, 2002
 */
public class CacheWatchRepairable implements ICacheObserver
{
    private final static Log log =
        LogFactory.getLog( CacheWatchRepairable.class );

    // the underlying ICacheObserver.
    private ICacheObserver cacheWatch;
    private Map cacheMap = new HashMap();

    /**
     * Replaces the underlying cache watch service and reattached all existing
     * listeners to the new cache watch.
     *
     * @param cacheWatch The new cacheWatch value
     */
    public void setCacheWatch( ICacheObserver cacheWatch )
    {
        this.cacheWatch = cacheWatch;
        synchronized ( cacheMap )
        {
            for ( Iterator itr = cacheMap.entrySet().iterator(); itr.hasNext();  )
            {
                Map.Entry entry = ( Map.Entry ) itr.next();
                String cacheName = ( String ) entry.getKey();
                Set listenerSet = ( Set ) entry.getValue();
                for ( Iterator itr2 = listenerSet.iterator(); itr2.hasNext();  )
                {
                    try
                    {
                        cacheWatch.addCacheListener( cacheName, ( ICacheListener ) itr2.next() );
                    }
                    catch ( IOException ex )
                    {
                        log.error( ex );
                    }
                }
            }
        }
    }


    /**
     * Adds a feature to the CacheListener attribute of the CacheWatchRepairable
     * object
     *
     * @param cacheName The feature to be added to the CacheListener attribute
     * @param obj The feature to be added to the CacheListener attribute
     */
    public void addCacheListener( String cacheName, ICacheListener obj )
        throws IOException
    {
        // Record the added cache listener locally, regardless of whether the remote add-listener
        // operation succeeds or fails.
        synchronized ( cacheMap )
        {
            Set listenerSet = ( Set ) cacheMap.get( cacheName );
            if ( listenerSet == null )
            {
                listenerSet = new HashSet();
                cacheMap.put( cacheName, listenerSet );
            }
            listenerSet.add( obj );
        }
        cacheWatch.addCacheListener( cacheName, obj );
    }


    /**
     * Adds a feature to the CacheListener attribute of the CacheWatchRepairable
     * object
     *
     * @param obj The feature to be added to the CacheListener attribute
     */
    public void addCacheListener( ICacheListener obj )
        throws IOException
    {
        // Record the added cache listener locally, regardless of whether the remote add-listener
        // operation succeeds or fails.
        synchronized ( cacheMap )
        {
            for ( Iterator itr = cacheMap.values().iterator(); itr.hasNext();  )
            {
                Set listenerSet = ( Set ) itr.next();
                listenerSet.add( obj );
            }
        }
        cacheWatch.addCacheListener( obj );
    }


    /** Description of the Method */
    public void removeCacheListener( String cacheName, ICacheListener obj )
        throws IOException
    {
        // Record the removal locally, regardless of whether the remote remove-listener
        // operation succeeds or fails.
        synchronized ( cacheMap )
        {
            Set listenerSet = ( Set ) cacheMap.get( cacheName );
            if ( listenerSet != null )
            {
                listenerSet.remove( obj );
            }
        }
        cacheWatch.removeCacheListener( cacheName, obj );
    }


    /** Description of the Method */
    public void removeCacheListener( ICacheListener obj )
        throws IOException
    {
        // Record the removal locally, regardless of whether the remote remove-listener
        // operation succeeds or fails.
        synchronized ( cacheMap )
        {
            for ( Iterator itr = cacheMap.values().iterator(); itr.hasNext();  )
            {
                Set listenerSet = ( Set ) itr.next();
                listenerSet.remove( obj );
            }
        }
        cacheWatch.removeCacheListener( obj );
    }
}