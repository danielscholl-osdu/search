package org.opengroup.osdu.search.context;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/**
 * Request-scoped context holding user authorization data.
 * 
 * <p><strong>Lifecycle:</strong>
 * <ul>
 *   <li>Created per HTTP request by Spring's request scope</li>
 *   <li>Populated by {@code AuthorizationFilter} during request authorization</li>
 *   <li>Consumed by query services during search operations</li>
 *   <li>Automatically destroyed at request completion</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong>
 * Request-scoped beans are thread-safe as each request thread gets its own instance.
 * The AuthorizationFilter always runs before query services, ensuring data is populated
 * before consumption.
 * 
 * <p><strong>Usage Contract:</strong>
 * - {@code dataGroups} may be null if not yet populated or if user has no groups
 * - {@code isRootUser} defaults to false until explicitly set
 * - Callers should handle null {@code dataGroups} gracefully
 */
@Component
@RequestScope
public class UserContext {
    private List<String> dataGroups;
    private boolean isRootUser;
    
    public List<String> getDataGroups() {
        return dataGroups;
    }
    
    public void setDataGroups(List<String> dataGroups) {
        this.dataGroups = dataGroups;
    }
    
    public boolean isRootUser() {
        return isRootUser;
    }
    
    public void setRootUser(boolean rootUser) {
        isRootUser = rootUser;
    }
}
