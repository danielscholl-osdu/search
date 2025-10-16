package org.opengroup.osdu.search.middleware;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.middleware.RequestHeadersTestUtil.setupRequestHeaderMock;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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

    	when(request.getHeader(anyString())).thenAnswer(invocation -> {
    		return headers.get(invocation.getArguments()[0]);
    	});
    }
}
