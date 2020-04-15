package org.opengroup.osdu.search.middleware;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.middleware.RequestHeadersTestUtil.setupRequestHeaderMock;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class RequestHeadersTestUtil {
	public static void setupRequestHeaderMock(Map<String, String> headers, HttpServletRequest request) {
    	// create an Enumeration over the header keys
    	Iterator<String> iterator = headers.keySet().iterator();
    	Enumeration<String> headerNames = new Enumeration<String>() {
    	    @Override
    	    public boolean hasMoreElements() {
    	        return iterator.hasNext();
    	    }

    	    @Override
    	    public String nextElement() {
    	        return iterator.next();
    	    }
    	};

    	when(request.getHeaderNames()).thenReturn(headerNames);
    	
    	when(request.getHeader(anyString())).thenAnswer(invocation -> {
    		return headers.get(invocation.getArguments()[0]);
    	});
    	
    	when(request.getHeaders(anyString())).thenAnswer(invocation -> {
    		
    		ArrayList<String> values = new ArrayList<String>();
    		values.add(headers.get(invocation.getArguments()[0]));
    		
    		
        	Iterator<String> it = values.iterator();
        	return new Enumeration<String>() {
        	    @Override
        	    public boolean hasMoreElements() {
        	        return it.hasNext();
        	    }

        	    @Override
        	    public String nextElement() {
        	        return it.next();
        	    }
        	};
    	});

    	
    	
    }
}
